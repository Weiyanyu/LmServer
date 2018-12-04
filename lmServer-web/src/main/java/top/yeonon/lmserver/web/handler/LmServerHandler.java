package top.yeonon.lmserver.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang3.StringUtils;
import top.yeonon.lmserver.web.http.LmRequest;
import top.yeonon.lmserver.web.http.LmResponse;
import top.yeonon.lmserver.web.http.LmWebRequest;
import top.yeonon.lmserver.web.method.MethodHandler;
import top.yeonon.lmserver.web.process.WebBeanProcessor;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * 属于Netty框架下的的handler，处于进站方向的最后一个
 *
 * @Author yeonon
 * @date 2018/5/23 0023 19:14
 **/
@ChannelHandler.Sharable
public class LmServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private LmServerHandler() {
    }

    public static final LmServerHandler INSTANCE = new LmServerHandler();

    private static final String STATIC_PATH = "static";

    //将对象转换成JSON格式的字符串需要用的
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        //构建LmRequest和LmResponse
        LmRequest request = LmRequest.build(ctx, fullHttpRequest);
        LmResponse response = LmResponse.build(ctx, request);
        //获取请求路径
        String path = request.getPath();
        if (StringUtils.isNotBlank(path) && path.endsWith(".html")) {
            sendHtml(response, path);
        } else {
            sendNormalContent(request, response, path);
        }

        ctx.channel().write(new LmWebRequest(request, response));
    }

    /**
     * 发送HTML静态内容
     *
     * @param response 响应
     * @param path     HTML所在路径
     */
    private void sendHtml(LmResponse response, String path) {
        String fileName = LmServerHandler.class.getResource("/").getPath() + STATIC_PATH + path;
        File file = new File(fileName);
        if (file.exists()) {
            response.setContent(file).setContentType(LmResponse.ContentTypeValue.HTML_CONTENT).send();
        } else {
            response.sendError("404 Not Found", HttpResponseStatus.NOT_FOUND);

        }
    }


    /**
     * 发送普通文本
     *
     * @param request  请求
     * @param response 响应
     * @param path     路径
     * @throws JsonProcessingException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void sendNormalContent(LmRequest request, LmResponse response, String path) throws JsonProcessingException, InvocationTargetException, IllegalAccessException {
        MethodHandler handler = WebBeanProcessor.getHandler(path.trim(), request.getMethod());
        //handler不为null的话，就正常执行url对应的处理方法，并将返回值写回客户端

        //这里实际上已经不需要判断handler是否为null，因为在之前的DispatchHandler已经处理过了
        Object message = handler.execute(request, response);
        if (message == null) {
            //如果消息为null，也许是参数错误，或者服务端出现异常，例如读写数据库异常等
            response.sendError("服务器异常或者参数错误", HttpResponseStatus.valueOf(500));
        } else {
            response.setContent(objectMapper.writeValueAsString(message))
                    .setContentType(LmResponse.ContentTypeValue.JSON_CONTENT)
                    .send();
        }


    }

    /**
     * 异常捕获，在Netty中，这应该是Pipeline中最后的Handler实现的方法
     *
     * @param ctx   ChannelHandlerContext
     * @param cause Throwable
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
