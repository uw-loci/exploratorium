
public abstract class Zprocessor {
	protected final Class<?> clazz = this.getClass();
	protected boolean drawOutput = true;
	public static FloatPoint[] results;
	public Zprocessor() {
		System.out.println(clazz.getName());
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(),
				url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);
	}
	public abstract FloatPoint[] process(String path);
}
