package com.chigix.resserver.sharablehandlers;

import com.chigix.resserver.domain.Bucket;
import com.chigix.resserver.domain.Resource;
import com.chigix.resserver.util.HttpHeaderNames;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.router.HttpRouted;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Context {

    private final HttpRouted routedInfo;

    private Resource resource;

    private final MessageDigest etagDigest;

    private final MessageDigest sha256Digest;

    private ByteBuf cachingChunkBuf;

    private final QueryStringDecoder queryDecoder;

    public Context(HttpRouted routedInfo, Resource resource) {
        try {
            this.etagDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        try {
            this.sha256Digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        this.routedInfo = routedInfo;
        this.resource = resource;
        this.queryDecoder = new QueryStringDecoder(routedInfo.getRequestMsg().uri());
        String content_type = routedInfo.getRequestMsg().headers().getAndConvert(HttpHeaderNames.CONTENT_TYPE);
        if (content_type != null) {
            resource.setMetaData("Content-Type", content_type);
        }
        String cache_control = routedInfo.getRequestMsg().headers().getAndConvert(HttpHeaderNames.CACHE_CONTROL);
        if (cache_control != null) {
            resource.setMetaData("Cache-Control", cache_control);
        }
        String content_disp = routedInfo.getRequestMsg().headers().getAndConvert(HttpHeaderNames.CONTENT_DISPOSITION);
        if (content_disp != null) {
            resource.setMetaData("Content-Disposition", content_disp);
        }
        String content_enc = routedInfo.getRequestMsg().headers().getAndConvert(HttpHeaderNames.CONTENT_ENCODING);
        if (content_enc != null) {
            resource.setMetaData("Content-Encoding", content_enc);
        }
        //@TODO: discuss later whether content-length is needed to extract.
    }

    public HttpRouted getRoutedInfo() {
        return routedInfo;
    }

    public QueryStringDecoder getQueryDecoder() {
        return queryDecoder;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        if (resource == null) {
            throw new InvalidParameterException();
        }
        this.resource = resource;
    }

    public MessageDigest getEtagDigest() {
        return etagDigest;
    }

    public MessageDigest getSha256Digest() {
        return sha256Digest;
    }

    public ByteBuf getCachingChunkBuf() {
        return cachingChunkBuf;
    }

    public void setCachingChunkBuf(ByteBuf cachingChunkBuf) {
        this.cachingChunkBuf = cachingChunkBuf;
    }

    public void copyTo(Context target) {
        target.setCachingChunkBuf(cachingChunkBuf);
        target.setResource(resource);
    }

    public static class UnpersistedResource extends Resource {

        private final Bucket bucket;

        public UnpersistedResource(Bucket bucket, String key) {
            super(key);
            this.bucket = bucket;
        }

        public UnpersistedResource(Bucket bucket, String key, String versionId) {
            super(key, versionId);
            this.bucket = bucket;
        }

        @Override
        public Bucket getBucket() {
            return bucket;
        }

    }

}
