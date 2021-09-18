package handler;

import bean.Message;
import netty.client.TcpClient809;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utill.Constants;
import utill.JT809Constants;

public class RecevieHandler extends SimpleChannelHandler {
	private static Logger LOG = LoggerFactory.getLogger(RecevieHandler.class);


	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e){
		Message msg = (Message) e.getMessage();
		LOG.info("应答----------------" + "0x" + Integer.toHexString(msg.getMsgId()));
		if(msg.getMsgId() == JT809Constants.UP_CONNECT_RSP){
			ChannelBuffer msgBody = msg.getMsgBody();
			int result = msgBody.readByte();
			if(result == JT809Constants.UP_CONNECT_RSP_SUCCESS){
				TcpClient809.LONGINSTATUS = Constants.LOGIN_STATAUS;
				LOG.info("------------------登录成功");
			}else{
				LOG.error("------------------登录异常，请检查" + "0x0" + Integer.toHexString(result));
			}
		}
	}
	public void channelConnected( ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		//连接
		LOG.info("channelConnected code:{}",ctx.hashCode());
		super.channelConnected(ctx, e);
	}
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		//断开
		LOG.info("channelDisconnected code:{}",ctx.hashCode());
		super.channelDisconnected(ctx, e);

	}
	public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		LOG.error("Unexpected exception from downstream:"+e.toString(), e);
		//super.exceptionCaught(ctx, e);
	}
}
