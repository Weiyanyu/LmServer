package top.yeonon.lmserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.apache.log4j.BasicConfigurator;
import top.yeonon.lmserver.handler.LmHandler;

import java.net.InetSocketAddress;


/**
 * @Author yeonon
 * @date 2018/5/20 0020 17:51
 **/
public class LmServerStarter {

    private Channel channel;
    private final EventLoopGroup group;
    private final ServerBootstrap serverBootstrap;
    private final int port;


    public LmServerStarter(int port) {
        this.port = port;
        group = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();
    }

    public ChannelFuture start() {
        serverBootstrap.group(group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
//                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new HttpObjectAggregator(16 * 1024));
                        pipeline.addLast(new LmHandler());
                    }
                });
        ChannelFuture future = serverBootstrap.bind(new InetSocketAddress(port));
        future.syncUninterruptibly();
        channel = future.channel();
        return future;
    }

    public void stop() {
        if (channel != null) {
            channel.closeFuture();
        }
        group.shutdownGracefully();
    }

    public static void run() {
        //log4j 配置项
        BasicConfigurator.configure();

        LmServerConfig serverConfig = new LmServerConfig();

        LmServerStarter starter = new LmServerStarter(serverConfig.getServerPort());
        ChannelFuture future = starter.start();

        Runtime.getRuntime().addShutdownHook(new Thread(starter::stop));

        future.channel().closeFuture().syncUninterruptibly();
    }
}
