import com.avolution.net.tcp.SimpleServerHandler;
import com.avolution.net.tcp.TCPPacketDecoder;
import com.avolution.net.tcp.TCPPacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import com.avolution.actor.Supervisor;
import com.avolution.actor.BasicActor;

public class TCPNettyService {

    private final int port;
    private final Supervisor supervisor;

    public TCPNettyService(int port) {
        this.port = port;
        this.supervisor = new Supervisor(Supervisor.SupervisionStrategy.RESTART);
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TCPPacketDecoder());  // 自定义解码器
                            ch.pipeline().addLast(new TCPPacketEncoder());  // 自定义编码器
                            SimpleServerHandler handler = new SimpleServerHandler();
//                            supervisor.addActor(handler.getActor());
                            ch.pipeline().addLast(handler);  // 业务处理器
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            System.out.println("TCPNettyService started and listening on port " + port);

            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int port = 8080;
        new TCPNettyService(port).start();
    }
}
