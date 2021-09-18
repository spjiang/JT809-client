package netty.client;

import bean.Idc2AwsGpsVo;
import bean.Message;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import utill.CRC16CCITT;
import utill.Constants;
import utill.JT809Constants;

import utill.Settings;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;

public class TcpClient809 {

	private static Logger LOG = LoggerFactory.getLogger(TcpClient809.class);
	public static int PLANT_CODE;//公司介入码
	public static int ZUCHE_ID_FUJIAN;//公司用户名
	public static String ZUCHE_PWD_FUJIAN;
	public static String LONGINSTATUS = "";
	public static String LOGINING = "logining";
	private static int LOGIN_FLAG = 0;
	private static String DOWN_LINK_IP = Settings.getStringValue("truck.server.tcp.ip");//初始化基类
	private static TcpClient tcpClient = TcpClient.getInstence();//初始化
	private static TcpClient809 tcpClient809 = new TcpClient809();
	//初始化channel,以便心跳机制及时登录
	private Channel channel =null;// tcpClient.getChannel(Constants.TCP_ADDRESS, Constants.TCP_PORT);

	public static TcpClient809 getInstance() {
		String localIp = "127.0.0.1";
		if (StringUtils.isNotBlank(localIp)) {
			PLANT_CODE = Settings.getIntValue("truck.server.plant.code");
			ZUCHE_ID_FUJIAN = Settings.getIntValue("truck.server.user.id");
			ZUCHE_PWD_FUJIAN =  Settings.getStringValue("truck.server.user.pwd");
		} else {
			LOG.error("获取本机IP异常");
		}
		return tcpClient809;
	}

	/**
	 * 判断是否登录 * boolean * @return
	 */
	public boolean isLogined() {
		return Constants.LOGIN_STATAUS.equals(LONGINSTATUS); //Constants常量类，自己随便定义就好，LOGIN_STATAUS="0x00"
	}

	/**
	 * 登录接入平台 * boolean * @return
	 */
	public boolean login2FuJianGov() {
		boolean success = false;
		if (!Constants.LOGIN_STATAUS.equals(LONGINSTATUS) && !LOGINING.equals(LONGINSTATUS)) {
			//开始登录 Message为数据对象，代码稍后给出
			Message msg = new Message(JT809Constants.UP_CONNECT_REQ);
			ChannelBuffer buffer = ChannelBuffers.buffer(46);
			//ByteBuf buffer=Unpooled.buffer(46);
			buffer.writeInt(ZUCHE_ID_FUJIAN);
			byte[] pwd = getBytesWithLengthAfter(8, ZUCHE_PWD_FUJIAN.getBytes());
			buffer.writeBytes(pwd);
			byte[] ip = getBytesWithLengthAfter(32, DOWN_LINK_IP.getBytes());
			buffer.writeBytes(ip);
			buffer.writeShort((short) Settings.getIntValue("truck.server.tcp.port"));//
			LOG.info("length:{}",buffer.array().length);
			msg.setMsgBody(buffer);
			channel = tcpClient.getChannel(Constants.TCP_ADDRESS, Constants.TCP_PORT);
			ChannelBuffer cb=buildMessage(msg);
			byte[] bs=cb.array();
			LOG.info("login length:{}",bs.length);
			channel.write(cb);
			//channel.wr
			LONGINSTATUS = LOGINING;
		}
		return success;
	}

	public static ChannelBuffer buildMessage(Message msg) {
		int bodyLength = 0;
		if (null != msg.getMsgBody()) {
			bodyLength = msg.getMsgBody().capacity();
		}
		LOG.info("-bodyLength:{},totallength:{}",bodyLength,bodyLength + Message.MSG_FIX_LENGTH);
		msg.setMsgGesscenterId(PLANT_CODE);
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(bodyLength + Message.MSG_FIX_LENGTH);
		ChannelBuffer headBuffer = ChannelBuffers.buffer(22);//---数据头
		headBuffer.writeInt(buffer.capacity());
		headBuffer.writeInt(msg.getMsgSn());
		headBuffer.writeShort((short) msg.getMsgId());
		headBuffer.writeInt(msg.getMsgGesscenterId());
		headBuffer.writeBytes(msg.getVersionFlag());
		headBuffer.writeByte(0);//加密
		headBuffer.writeInt(10);
		buffer.writeBytes(headBuffer);//---数据体
		if (null != msg.getMsgBody()) {
			buffer.writeBytes(msg.getMsgBody());
		}
		ChannelBuffer finalBuffer = ChannelBuffers.copiedBuffer(buffer);//--crc校验码
		byte[] b = ChannelBuffers.buffer(finalBuffer.capacity()).array();
		finalBuffer.getBytes(0, b);
		int crcValue = CRC16CCITT.crc16(b);
		finalBuffer.writeShort((short) crcValue);//2//转义
		byte[] bytes = ChannelBuffers.copiedBuffer(finalBuffer).array();
		ChannelBuffer headFormatedBuffer = ChannelBuffers.dynamicBuffer(finalBuffer.capacity());
		formatBuffer(bytes, headFormatedBuffer);
		ChannelBuffer buffera = ChannelBuffers.buffer(headFormatedBuffer.capacity() + 2);
		buffera.writeByte(Message.MSG_HEAD);
		buffera.writeBytes(headFormatedBuffer);
		buffera.writeByte(Message.MSG_TALL);
		LOG.info("-bufferaLength:{}",buffera.capacity());
		return ChannelBuffers.copiedBuffer(buffera);
	}

	/**
	 * 发送数据到接入平台 * boolean * @param awsVo 是上层程序得到的带发送的数据对象，可以看自己的需求，替换 * @return
	 */
	public boolean sendMsg2FuJianGov(Idc2AwsGpsVo awsVo) {
		boolean success = false;
		if (isLogined()) {
			//已经登录成功，开始发送数据
			LOG.info("开始发送数据");
			channel = tcpClient.getChannel(Constants.TCP_ADDRESS, Constants.TCP_PORT);
			if (null != channel && channel.isWritable()) {
				Message msg = buildSendVO(awsVo);
				ChannelBuffer msgBuffer = buildMessage(msg);
				byte[] bs=msgBuffer.array();
				LOG.info("send length:{},{},{},{}",msgBuffer.readableBytes(),msgBuffer.capacity(),bs.length,bs[89]+","+bs[90]);
				channel.write(msgBuffer);
			} else {
				LONGINSTATUS = "";
			}
		} else if (
				LOGIN_FLAG == 0) {
			LOGIN_FLAG++;
			login2FuJianGov();
			LOG.info("--------------第一次登录");
		} else {
			LOG.info("--------------等待登录");
		}
		return success;
	}

	/**
	 * 转换VO * void * @param awsVo
	 */
	private Message buildSendVO(Idc2AwsGpsVo awsVo) {
		Message msg = new Message(JT809Constants.UP_EXG_MSG);
		ChannelBuffer buffer = ChannelBuffers.buffer(36);
		buffer.writeByte((byte) 0);//是否加密,0未加密 ,1加密
		Calendar cal = Calendar.getInstance();//日月年dmyy
		cal.setTime(new Date());
		buffer.writeByte((byte) cal.get(Calendar.DATE));
		buffer.writeByte((byte) (cal.get(Calendar.MONTH) + 1));
		String hexYear = "0" + Integer.toHexString(cal.get(Calendar.YEAR));
		buffer.writeBytes(hexStringToByte(hexYear));//4//
		buffer.writeByte((byte) cal.get(Calendar.HOUR));
		buffer.writeByte((byte) cal.get(Calendar.MINUTE));
		buffer.writeByte((byte) cal.get(Calendar.SECOND));//3//时分秒
		buffer.writeInt(formatLonLat(awsVo.getLon()));//4经度
		buffer.writeInt(formatLonLat(awsVo.getLat()));//4//纬度
		buffer.writeShort(awsVo.getSpeed());//2//速度
		buffer.writeShort(awsVo.getSpeed2());//2//行驶记录速度
		buffer.writeInt(awsVo.getMileage());//4//车辆当前总里程数
		buffer.writeShort(awsVo.getDirection());//2//方向
		buffer.writeShort((short) 10);//2//海拔
		buffer.writeInt(1);//4//车辆状态
		buffer.writeInt(1);//报警状态 0表示正常；1表示报警//4
		ChannelBuffer headBuffer = ChannelBuffers.buffer(buffer.capacity() + 28);
		try {
			headBuffer.writeBytes(getBytesWithLengthAfter(21, awsVo.getVehicleNo().getBytes("gbk")));
		} catch (UnsupportedEncodingException e) {
		}//21 车牌号
		headBuffer.writeByte((byte) 1);//1 车牌颜色：注意不是车身颜色
		headBuffer.writeShort(JT809Constants.UP_EXG_MSG_REAL_LOCATION);//2 子业务码
		headBuffer.writeInt(buffer.capacity());
		headBuffer.writeBytes(buffer);
		LOG.info("send length:"+headBuffer.capacity());
		msg.setMsgBody(headBuffer);
		return msg;
	}

	/**
	 * 报文转义 * void * @param bytes * @param formatBuffer
	 */
	private static void formatBuffer(byte[] bytes, ChannelBuffer formatBuffer) {
		for (byte b : bytes) {
			switch (b) {
			case 0x5b:
				byte[] formatByte0x5b = new byte[2];
				formatByte0x5b[0] = 0x5a;
				formatByte0x5b[1] = 0x01;
				formatBuffer.writeBytes(formatByte0x5b);
				break;
			case 0x5a:
				byte[] formatByte0x5a = new byte[2];
				formatByte0x5a[0] = 0x5a;
				formatByte0x5a[1] = 0x02;
				formatBuffer.writeBytes(formatByte0x5a);
				break;
			case 0x5d:
				byte[] formatByte0x5d = new byte[2];
				formatByte0x5d[0] = 0x5e;
				formatByte0x5d[1] = 0x01;
				formatBuffer.writeBytes(formatByte0x5d);
				break;
			case 0x5e:
				byte[] formatByte0x5e = new byte[2];
				formatByte0x5e[0] = 0x5e;
				formatByte0x5e[1] = 0x02;
				formatBuffer.writeBytes(formatByte0x5e);
				break;
			default:
				formatBuffer.writeByte(b);
				break;
			}
		}
	}

	/**
	 * 16进制字符串转换成byte数组 * byte[] * @param hex
	 */
	public static byte[] hexStringToByte(String hex) {
		hex = hex.toUpperCase();
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		return result;
	}

	private static byte toByte(char c) {
		byte b = (byte) "0123456789ABCDEF".indexOf(c);
		return b;
	}

	/**
	 * 格式化经纬度,保留六位小数 * int * @param needFormat * @return
	 */
	private static int formatLonLat(Double needFormat) {
		NumberFormat numFormat = NumberFormat.getInstance();
		numFormat.setMaximumFractionDigits(6);
		numFormat.setGroupingUsed(false);
		String fristFromat = numFormat.format(needFormat);
		Double formatedDouble = Double.parseDouble(fristFromat);
		numFormat.setMaximumFractionDigits(0);
		String formatedValue = numFormat.format(formatedDouble * 1000000);
		return Integer.parseInt(formatedValue);
	}

	/**
	 * 补全位数不够的定长参数 有些定长参数，实际值长度不够，在后面补0x00 * byte[] * @param length * @param pwdByte * @return
	 */
	private static byte[] getBytesWithLengthAfter(int length, byte[] pwdByte) {
		byte[] lengthByte = new byte[length];
		for (int i = 0; i < pwdByte.length; i++) {
			lengthByte[i] = pwdByte[i];
		}
		for (int i = 0; i < (length - pwdByte.length); i++) {
			lengthByte[pwdByte.length + i] = 0x00;
		}
		return lengthByte;
	}

	public static void main(String[] args) {
		TcpClient809 s = TcpClient809.getInstance();
		Idc2AwsGpsVo awsVo = new Idc2AwsGpsVo();
		awsVo.setDirection((short) 12);
		awsVo.setLon(117.290092);
		awsVo.setLat(39.56362);
		awsVo.setSpeed((short) 45);
		awsVo.setSpeed2((short) 23);
		awsVo.setMileage(10001);
		awsVo.setDirection((short) 1);
		awsVo.setVehicleNo("幽123D32");
		LOG.info("开始send message--ff");
		s.sendMsg2FuJianGov(awsVo);
		try {
			Thread.sleep(2 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for(int i=0;i<1;i++) {
			s.sendMsg2FuJianGov(awsVo);
		}

	}
}
