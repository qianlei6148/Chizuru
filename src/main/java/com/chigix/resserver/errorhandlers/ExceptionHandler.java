package com.chigix.resserver.errorhandlers;

import com.chigix.resserver.ApplicationContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.router.HttpException;
import io.netty.handler.codec.http.router.SimpleHttpExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html
 *
 * @author Richard Lea <chigix@zoho.com>
 */
@ChannelHandler.Sharable
public class ExceptionHandler extends SimpleHttpExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class.getName());

    private final ApplicationContext application;

    private static ExceptionHandler INSTANCE = null;

    public static ExceptionHandler getInstance(ApplicationContext ctx) {
        if (INSTANCE == null) {
            INSTANCE = new ExceptionHandler(ctx);
        }
        return INSTANCE;
    }

    public ExceptionHandler(ApplicationContext application) {
        this.application = application;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HttpException msg) throws Exception {
        LOG.error(msg.getMessage(), msg.getCause());
        super.messageReceived(ctx, msg);
    }

}
