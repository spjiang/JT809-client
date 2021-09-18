package utill;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * 资源控制器
 * @author gxp
 *
 */
public class ResourceControl extends Control {
	public ResourceControl() {
		super();
	}

	@Override
	public long getTimeToLive(String baseName, Locale locale) {
		return 5*1000;// 5秒
	}

	@Override
	public boolean needsReload(String baseName, Locale locale, String format,
			ClassLoader loader, ResourceBundle bundle, long loadTime) {
		return true;
	}
}
