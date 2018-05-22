package top.yeonon.lmserver.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author yeonon
 * @date 2018/5/20 0020 20:26
 **/
public class LmResponse {

    /**
     * Content-Type的值常量
     */
    public interface ContentTypeValue {
        //内容类型是HTML
        String HTML_CONTENT = "text/html;charset=utf-8";
        //内容类型是纯文本
        String PLAIN_CONTENT = "text/plain;charset=utf-8";
        //内容类型是JSON
        String JSON_CONTENT = "application/json;charset=utf-8";
        //内容类型是XML
        String XML_CONTENT = "text/xml;charset=utf-8";
        //内容类型是JavaScript脚本
        String JAVASCRIPT_CONTENT = "application/javascript;charset=utf-8";
    }



    private Object content = Unpooled.EMPTY_BUFFER;
    private HttpHeaders headers = new DefaultHttpHeaders();
    private Set<Cookie> cookies = new HashSet<>();
    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private String contentType = ContentTypeValue.PLAIN_CONTENT;


    private ChannelHandlerContext ctx;
    private LmRequest lmRequest;
    private boolean isSent;

    private LmResponse(ChannelHandlerContext ctx, LmRequest lmRequest) {
        this.ctx = ctx;
        this.lmRequest = lmRequest;

        //默认添加一些请求头
        headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }


    /**
     * 设置Content-Type
     * @param contentType 字符串
     */
    public LmResponse setContentType(String contentType) {
        this.contentType = contentType;
        headers.set(HttpHeaderNames.CONTENT_TYPE, this.contentType);
        return this;
    }

    /**
     * 设置响应状态码
     * @param status 状态码
     * @return 本身
     */
    public LmResponse setStatus(HttpResponseStatus status) {
        this.status = status;
        return this;
    }

    /**
     * 设置响应状态码（使用数字，例如200）
     * @param statusCode 响应码的数字表示
     * @return 本身
     */
    public LmResponse setStatus(int statusCode) {
        this.status = HttpResponseStatus.valueOf(statusCode);
        return this;
    }

    /**
     * 设置响应头字段（重复的会覆盖值）
     * @param headerName 字段名
     * @param headerValue 字段值
     * @return 本身
     */
    public LmResponse setHeaders(String headerName, String headerValue) {
        this.headers.set(headerName, headerValue);
        return this;
    }

    /**
     * 设置响应头字段
     * @param headerName 字段名
     * @param headerValue 字段值
     * @return 本身
     */
    public LmResponse addHeaders(String headerName, String headerValue) {
        this.headers.add(headerName, headerValue);
        return this;
    }

    /**
     * 设置响应头集合
     * @param headers 用户可以自己构造HttpHeaders，然后一次传递进来
     * @return 本身
     */
    public LmResponse addHeaders(HttpHeaders headers) {
        this.headers.add(headers);
        return this;
    }

    /**
     * 设置Content-Length
     * @param contentLength 长度
     * @return 本身
     */
    public LmResponse setContentLength(long contentLength) {
        this.headers.set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
        return this;
    }

    /**
     * 设置长连接
     * @return 本身
     */
    public LmResponse setKeepAlive() {
        this.headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        return this;
    }

    public LmResponse setHttpVersion(HttpVersion version) {
        this.httpVersion = version;
        return this;
    }

    /**
     * 添加cookie
     * @param cookie 标准cookie对象
     * @return 本身
     */
    public LmResponse addCookie(Cookie cookie) {
        this.cookies.add(cookie);
        return this;
    }

    /**
     * 添加cookie
     * @param cookieName 字段名
     * @param cookieValue 字段值
     * @return 本身
     */
    public LmResponse addCookie(String cookieName, String cookieValue) {
        this.addCookie(new DefaultCookie(cookieName, cookieValue));
        return this;
    }

    /**
     * 设置返回文本内容，基于Netty，故使用ByteBuf包装
     * @param content 返回内容
     * @return 本身
     */
    public LmResponse setContent(String content) {
        this.content = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
        return this;
    }

    /**
     * 设置返回的Buffer
     * @param content ByteBuf类型
     * @return 本身
     */
    public LmResponse setContent(ByteBuf content) {
        this.content = content;
        return this;
    }

    /**
     * 设置返回的Buffer
     * @param content ByteBuf类型
     * @return 本身
     */
    public LmResponse setContent(byte[] content) {
        this.content = Unpooled.copiedBuffer(content);
        return this;
    }

    /**
     * 转换成Netty支持的Response
     * @return response
     */
    private FullHttpResponse toFullHttpResponse() {
        ByteBuf buf = (ByteBuf) content;
        final FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion, status, buf);

        //设置Headers
        final HttpHeaders httpHeaders = fullHttpResponse.headers();
        setContentLength(buf.readableBytes());
        httpHeaders.add(this.headers);

        //设置cookies
        for (Cookie cookie : this.cookies) {
            httpHeaders.add(HttpHeaderNames.SET_COOKIE.toString(), ServerCookieEncoder.LAX.encode(cookie));
        }

        return fullHttpResponse;
    }

    /**
     * 向客户端发送消息
     * @return ChannelFuture
     */
    public ChannelFuture send() {
        ChannelFuture future = sendFull();
        this.isSent = true;
        return future;
    }

    /**
     * 发送文本内容给客户端（文本内容一般使用FullHttpResponse）
     * @return ChannelFuture
     */
    private ChannelFuture sendFull() {
        if (lmRequest.isKeepAlive()) {
            setKeepAlive();
        }
        ctx.write(toFullHttpResponse());
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        if (!lmRequest.isKeepAlive()) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

        return future;
    }


    public boolean isSent() {
        return isSent;
    }



    public static LmResponse build(ChannelHandlerContext ctx, LmRequest lmRequest) {
        return new LmResponse(ctx, lmRequest);
    }


}
