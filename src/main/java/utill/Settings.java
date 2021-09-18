package utill;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 配置工具类
 * 
 * @modificationHistory.  
 * <ul>
 * <li>liqg 2013-4-26下午2:19:23 TODO</li>
 * </ul>
 */
public class Settings {
	private final static Logger log = LoggerFactory.getLogger(Settings.class);
	private static ResourceBundle SETTINGS;
	private static File file = null;
	private static ResourceControl rc = new ResourceControl();
	private static long lastModified = 0;
	private static String filePathName="setting";
	static {
		SETTINGS = ResourceBundle.getBundle(filePathName, rc);
		try {
			file = new File(Settings.class.getResource("/"+filePathName+".properties").toURI());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (file != null) {
						try {
							if (file.lastModified() != lastModified) {
								SETTINGS = ResourceBundle.getBundle(filePathName, rc);
								lastModified = file.lastModified();
							}
						} catch (Throwable t) {
							log.error(t.getMessage());
						}
						try {
							Thread.sleep(60*1000);
						} catch (InterruptedException e) {
						} //休眠60秒
					} else {
						log.error("Not found the file of '/"+filePathName+".properties'.");
						break;
					}
				}
			}
		});
		t.start();
	}
	public static int getIntValue(String key){
		return Integer.parseInt(SETTINGS.getString(key));
	}
	public static String getStringValue(String key){
		String str=null;
		try {
			str =new String(SETTINGS.getString(key).getBytes("ISO-8859-1"), "utf-8");
		} catch (Exception e) {

		}
		return StringUtils.isNotEmpty(str)?str.trim():str;
	}
	public static boolean getBooleanValue(String key){
		return Boolean.parseBoolean(SETTINGS.getString(key));
	}
}
