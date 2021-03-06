package com.chigix.resserver.endpoint.PostResource;

import com.chigix.resserver.Application;
import com.chigix.resserver.config.ApplicationContext;
import com.chigix.resserver.domain.error.InvalidPart;
import com.chigix.resserver.domain.error.NoSuchBucket;
import com.chigix.resserver.domain.model.chunk.Chunk;
import com.chigix.resserver.domain.model.multiupload.MultipartUploadRepository;
import com.chigix.resserver.domain.model.resource.AmassedResource;
import com.chigix.resserver.domain.model.resource.ChunkedResource;
import com.chigix.resserver.domain.model.resource.ResourceRepository;
import com.chigix.resserver.domain.model.resource.SpecificationFactory;
import com.chigix.resserver.interfaces.io.IteratorInputStream;
import com.chigix.resserver.interfaces.xml.XPathNode;
import com.chigix.resserver.scheduledtask.ReadStreamTaskHooker;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
@ChannelHandler.Sharable
class MultiUploadCompleteContentHandler extends MultiUploadCompleteHandler.Content {

    @Autowired
    private ApplicationContext application;

    @Autowired
    private SpecificationFactory specifications;

    private static final Logger LOG = LoggerFactory.getLogger(MultiUploadCompleteContentHandler.class.getName());

    public MultiUploadCompleteContentHandler() {
        // Intentionally Empty.
    }

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, ByteBuf bytebuf,
            final MultipartUploadContext routing_ctx) throws Exception {
        byte[] bytes = new byte[bytebuf.readableBytes()];
        bytebuf.readBytes(bytes);
        routing_ctx.getXmlStreamFeeder().feedInput(bytes, 0, bytes.length);
        final XMLStreamReader reader = routing_ctx.getXmlStreamReader();
        int e;
        while (reader.hasNext()) {
            switch (e = reader.next()) {
                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    routing_ctx.clearCurrentXmlStreamTextCache();
                    XPathNode new_node = new XPathNode(reader.getLocalName(),
                            routing_ctx.getCurrentXmlStreamNode());
                    routing_ctx.setCurrentXmlStreamNode(new_node);
                    tryInstantiateContextPart(new_node.getSimplePath(), routing_ctx);
                    continue;
                case XMLStreamConstants.END_ELEMENT:
                    final XPathNode node = routing_ctx.getCurrentXmlStreamNode();
                    if (node == null) {
                        continue;
                    }
                    routing_ctx.setCurrentXmlStreamNode(node.getParent());
                    if (tryUpdatePartEtag(node.getSimplePath(), routing_ctx,
                            routing_ctx.getCurrentXmlStreamTextCache().toString())) {
                        continue;
                    } else if (tryUpdatePartNumber(node.getSimplePath(), routing_ctx,
                            routing_ctx.getCurrentXmlStreamTextCache().toString())) {
                        continue;
                    } else if (tryAppendChunkResourceReading(
                            node.getSimplePath(), routing_ctx,
                            application.getEntityManager().getUploadRepository(),
                            application.getEntityManager().getResourceRepository())) {
                        continue;
                    } else if (tryExecuteCalculator(node.getSimplePath(), routing_ctx)) {
                        continue;
                    }
                    continue;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    LOG.warn("PROCESSING_INSTRUCTION is not handled in MultiUploadCompleteContentHandler.");
                    continue;
                case XMLStreamConstants.CHARACTERS:
                    routing_ctx.getCurrentXmlStreamTextCache().write(reader.getText());
                    LOG.info(routing_ctx.getCurrentXmlStreamNode().getSimplePath());
                    continue;
                case XMLStreamConstants.SPACE:
                    LOG.warn("SPACE is not handled in MultiUploadCompleteContentHandler.");
                    continue;
                case XMLStreamConstants.START_DOCUMENT:
                    // @TODO: application.getEntityManager().getResourceRepository()
                    //        .removeSubResource(routing_ctx.getResource());
                    routing_ctx.getCurrentEtagCalculator().onFinished = (Callable) () -> {
                        ctx.fireChannelRead(routing_ctx);
                        return null;
                    };
                    continue;
                case XMLStreamConstants.END_DOCUMENT:
                    return;
                default:
                    throw new Exception("Unknown XML Event in completing Upload: " + e);
            }
        }
    }

    private boolean tryInstantiateContextPart(String xpath, MultipartUploadContext ctx) {
        if ("/CompleteMultipartUpload/Part".equals(xpath)) {
            ctx.newCurrentXmlStreamUploadPart();
            return true;
        }
        return false;
    }

    private boolean tryUpdatePartNumber(String xpath, MultipartUploadContext ctx, String number) {
        if ("/CompleteMultipartUpload/Part/PartNumber".equals(xpath)) {
            ctx.getCurrentXmlStreamUploadPart().setPartNumber(number);
            return true;
        }
        return false;
    }

    private boolean tryUpdatePartEtag(String xpath, MultipartUploadContext ctx, String etag) {
        if ("/CompleteMultipartUpload/Part/ETag".equals(xpath)) {
            ctx.getCurrentXmlStreamUploadPart().setEtag(etag);
            return true;
        }
        return false;
    }

    private boolean tryAppendChunkResourceReading(String xpath, MultipartUploadContext ctx, MultipartUploadRepository uploaddao, ResourceRepository resourcedao) throws InvalidPart, NoSuchBucket {
        if ("/CompleteMultipartUpload/Part".equals(xpath)) {
            final AmassedResource amassed_resource = ctx.getResource();
            final MultipartUploadContext.CompleteMultipartUploadPart part = ctx.getCurrentXmlStreamUploadPart();
            final ChunkedResource part_resource = uploaddao.findUploadPart(ctx.getUpload(),
                    part.getPartNumber(),
                    part.getEtag());
            ctx.getCurrentEtagCalculator().appendChunkResource(part_resource);
            final BigInteger prev_size = new BigInteger(amassed_resource.getSize());
            final BigInteger updated_size = prev_size.add(new BigInteger(part_resource.getSize()));
            amassed_resource.setSize(updated_size.toString());
            resourcedao.insertSubresource(part_resource,
                    specifications.subresourceInfo(
                            amassed_resource,
                            prev_size, updated_size, part.getPartNumber()));
            return true;
        }
        return false;
    }

    private boolean tryExecuteCalculator(String xpath, MultipartUploadContext ctx) {
        if ("/CompleteMultipartUpload".equals(xpath)) {
            ctx.getCurrentEtagCalculator().endLock();
            Application.amassedResourceFileReadingPool.execute(ctx.getCurrentEtagCalculator());
            return true;
        }
        return false;
    }

    public static class CalculateEtag implements Runnable {

        private Callable onFinished = null;

        private final List<ReadStreamTaskHooker> chunkReaders = new LinkedList<>();

        private boolean chunkReadersLocked = false;

        private final MultipartUploadContext routingContext;

        public CalculateEtag(MultipartUploadContext ctx) {
            this.routingContext = ctx;
        }

        @Override
        public void run() {
            chunkReaders.forEach((chunkReader) -> {
                chunkReader.hookTask((buffer) -> {
                    routingContext.getEtagDigest().update(buffer);
                });
                chunkReader.run();
            });
            if (chunkReadersLocked) {
                try {
                    onFinished.call();
                } catch (Exception ex) {
                    throw new RuntimeException("Unexpected Exceptions when AmassedResource Files totally scanned.", ex);
                }
            }
        }

        public void appendChunkResource(ChunkedResource r) {
            // getChunks implementation in this project is designed as a iterator 
            // proxy fetching database lazily.
            chunkReaders.add(new ReadStreamTaskHooker(new IteratorInputStream<Chunk>(r.getChunks()) {
                @Override
                protected InputStream inputStreamProvider(Chunk item) throws NoSuchElementException {
                    try {
                        return item.getInputStream();
                    } catch (IOException ex) {
                        LOG.error("Unexpected!!!! IOException occurs when reading chunk stream: ["
                                + item.getContentHash() + "]", ex);
                        throw new NoSuchElementException();
                    }
                }
            }));
        }

        public void endLock() {
            chunkReadersLocked = true;
        }
    }

}
