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
import com.avolution.service.IService;

public class TCPNettyService implements IService {

    private final int port;
    private final Supervisor supervisor;
    private volatile Status status;

    public TCPNettyService(int port) {
        this.port = port;
        this.supervisor = new Supervisor(Supervisor.SupervisionStrategy.RESTART);
        this.status = Status.STOPPED;
    }

    @Override
    public void start() {
        if (status == Status.RUNNING || status == Status.STARTING) {
            return;
        }
        status = Status.STARTING;
        new Thread(() -> {
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
                                ch.pipeline().addLast(handler);  // 业务处理器
                            }
                        });

                ChannelFuture f = b.bind(port).sync();
                System.out.println("TCPNettyService started and listening on port " + port);
                status = Status.RUNNING;

                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                status = Status.ERROR;
                Thread.currentThread().interrupt();
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                status = Status.STOPPED;
            }
        }).start();
    }

    @Override
    public void pause() {
        if (status == Status.RUNNING) {
            status = Status.PAUSED;
            // Implement pause logic if needed
        }
    }

    @Override
    public void stop() {
        if (status == Status.RUNNING || status == Status.PAUSED) {
            status = Status.STOPPING;
            // Implement stop logic if needed
            status = Status.STOPPED;
        }
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public String getStatusInfo() {
        return "TCPNettyService is currently " + status;
    }

    public static void main(String[] args) {
        int port = 8080;
        TCPNettyService service = new TCPNettyService(port);
        service.start();
    }
}
