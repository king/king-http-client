package com.king.platform.net.http.netty.response;

import com.king.platform.net.http.ResponseBodyConsumer;
import com.king.platform.net.http.netty.BaseHttpRequestHandler;
import com.king.platform.net.http.netty.HttpRequestContext;
import com.king.platform.net.http.netty.eventbus.RequestEventBus;
import com.king.platform.net.http.netty.metric.TimeStampRecorder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.mockito.Mockito.*;

public class HttpClientResponseHandlerTest {

    /**
     * EBNF spec of Content-Type header:
     *
     *  media-type = type "/" subtype *( OWS ";" OWS parameter )
     *  type       = token
     *  subtype    = token
     *  parameter      = token "=" ( token / quoted-string )
     *
     *  See  <a href="https://www.rfc-editor.org/rfc/rfc7231#section-3.1.1.5">RFC7231 - Section 3.1.1.5</a>
     */
    @ParameterizedTest
    @CsvSource(value = {
            "text/plain; charset=utf-8:text/plain:utf-8",
            "text/plain;charset=ISO-8851-1:text/plain:ISO-8851-1",
            "text/plain;charset=ISO-8851-1:text/plain:ISO-8851-1",
            "text/plain;charset=\"ISO-8851-1\":text/plain:ISO-8851-1",
            "text/plain; version=0.0.4; charset=utf-8:text/plain:utf-8"
    }, delimiter = ':')
    public void handle_directives_in_ContentType(String contentTypeHeader, String contentType, String charset) throws Exception {

        HttpRedirector mockRedirector = mock(HttpRedirector.class);
        HttpClientResponseHandler handler = new HttpClientResponseHandler(mockRedirector);

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel mockChannel = mock(Channel.class);
        Attribute<HttpRequestContext> mockCtxAttr = mock(Attribute.class);
        HttpRequestContext mockRequestCtx = mock(HttpRequestContext.class);
        Attribute<Boolean> mockErrorAttr = mock(Attribute.class);
        NettyHttpClientResponse mockNettyHttpClientResp = mock(NettyHttpClientResponse.class);
        RequestEventBus mockRequestEventBus = mock(RequestEventBus.class);
        ResponseBodyConsumer mockRespBodyConsumer = mock(ResponseBodyConsumer.class);

        when(ctx.channel()).thenReturn(mockChannel);

        when(mockChannel.attr(HttpRequestContext.HTTP_REQUEST_ATTRIBUTE_KEY)).thenReturn(mockCtxAttr);
        when(mockCtxAttr.get()).thenReturn(mockRequestCtx);

        when(mockChannel.attr(BaseHttpRequestHandler.HTTP_CLIENT_HANDLER_TRIGGERED_ERROR)).thenReturn(mockErrorAttr);
        when(mockErrorAttr.get()).thenReturn(false);

        when(mockRequestCtx.hasCompletedContent()).thenReturn(false);

        when(mockRequestCtx.getNettyHttpClientResponse()).thenReturn(mockNettyHttpClientResp);
        when(mockNettyHttpClientResp.getRequestEventBus()).thenReturn(mockRequestEventBus);
        when(mockNettyHttpClientResp.getResponseBodyConsumer()).thenReturn(mockRespBodyConsumer);

        HttpResponse msg = mock(HttpResponse.class);
        DecoderResult decodeRes = mock(DecoderResult.class);
        when(msg.decoderResult()).thenReturn(decodeRes);
        when(decodeRes.isFailure()).thenReturn(false);
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(HttpHeaderNames.CONTENT_TYPE, contentTypeHeader);
        when(msg.headers()).thenReturn(headers);

        TimeStampRecorder mockTimeRecorder = mock(TimeStampRecorder.class);
        when(mockRequestCtx.getTimeRecorder()).thenReturn(mockTimeRecorder);

        when(mockRequestCtx.getHttpMethod()).thenReturn(HttpMethod.GET);
        when(msg.status()).thenReturn(HttpResponseStatus.OK);
        handler.handleResponse(ctx, msg);

        verify(mockRespBodyConsumer, times(1)).onBodyStart(contentType, charset, 0);
    }
}
