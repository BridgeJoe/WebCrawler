import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler {	
	private ArrayList<String> toCrawl = new ArrayList<String>();
	private ArrayList<String> allUrls = new ArrayList<String>();
	private ArrayList<String> keyWord = new ArrayList<String>();;
	private Hashtable<String, Integer> urlsDepth = new Hashtable<String, Integer>();
	
	
	//运行参数
	private String startingUrl = "http://www.baidu.com";
	private int webDepth = 2;
	private int threadNumber = 8;
	private String regKeyword = "([\\u4E00-\\u9FA5]*特朗普[\\u4E00-\\u9FA5]*)";
	private String regUrl = "(http://(www.){0,1}[a-z0-9A-Z.]+com)";
	
	
	private String urlreport = "Url：" + startingUrl + " 深度为0" + "\r\n";
	private String keywordreport = "";
	SimpleDateFormat formatter = new SimpleDateFormat("HH时mm分ss秒");
	
	
	private synchronized void addUrl(String url) {
		synchronized (this) {
			toCrawl.add(toCrawl.size(), url);
			allUrls.add(allUrls.size(), url);
		}
	}
	private synchronized String getUrl() {
		synchronized (this) {
			String temp = toCrawl.get(0);
			toCrawl.remove(0);
			return temp;
		}
	}
	private boolean hasNextUrl() {
		return !toCrawl.isEmpty();
	}
	
	
	private void connectStartingUrl() throws IOException {
		long startTime = System.currentTimeMillis();
		System.out.println("连接初始Url：" + startingUrl);
		urlsDepth.put(startingUrl, 0);
		addUrl(startingUrl);
		getUrl();
		URL url = new URL(startingUrl);
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		InputStreamReader isr = new InputStreamReader(url.openStream());
		BufferedReader br = new BufferedReader(isr);
		Pattern pUrl = Pattern.compile(regUrl);
		Matcher m;
		//读取网页
		String tempStr = br.readLine();
		while (tempStr != null)
		{
			searchKeyword(tempStr, startingUrl);
			m = pUrl.matcher(tempStr);
			while (m.find())
			{
				String temp = m.group();
				if(!allUrls.contains(temp))
				{
					addUrl(temp);
					urlsDepth.put(temp, 1);
					addReport(temp, "深度为1");
				}
			}
			tempStr = br.readLine();
		}
		isr.close();
		for (int i = 0; i < threadNumber; i++) {
			new Thread(new myThread(this)).start();
		}
		while(true)
		{
			if (!this.hasNextUrl() && Thread.activeCount() == 1) {
				System.out.print("");
				long finishTime = System.currentTimeMillis();
				long costTime = finishTime - startTime;
				System.out.println("\n\n运行完毕！总计爬行" + this.allUrls.size() + "个网页");
				Date start = new Date(startTime);
				Date finish = new Date(finishTime);
				System.out.println("开始时间：" + formatter.format(start));
				System.out.println("结束时间：" + formatter.format(finish));
				System.out.println("爬行时间：" + costTime / 1000.00 + "秒");
				break;
			}
			else
				System.out.print("");
		}
	}
	private void connectUrl() throws IOException {
		//连接网页
		String str = getUrl();
		if(urlsDepth.get(str) > webDepth)
			return;
		System.out.println("正在爬行：" + str + " 深度为：" + urlsDepth.get(str));
		URL url = new URL(str);
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		InputStreamReader isr = new InputStreamReader(url.openStream());
		BufferedReader br = new BufferedReader(isr);
		Pattern pUrl = Pattern.compile(regUrl);
		Matcher m;
		//读取网页
		String tempStr = br.readLine();
		while (tempStr != null)
		{
			searchKeyword(tempStr, str);
			m = pUrl.matcher(tempStr);
			while (m.find())
			{
				if(urlsDepth.get(str) + 1 > webDepth)
					break;
				String temp = m.group();
				if(!allUrls.contains(temp))
				{
					addUrl(temp);
					urlsDepth.put(temp, (urlsDepth.get(str) + 1));
					addReport(temp, "深度为" + (urlsDepth.get(str) + 1));
				}
			}
			tempStr = br.readLine();
		}
		isr.close();
	}
	
	
	private void searchKeyword(String str, String url) throws IOException {
		Pattern pKeyword = Pattern.compile(regKeyword);
		Matcher m;
		m = pKeyword.matcher(str);
		while (m.find())
		{
			String s = m.group();
			if(!keyWord.contains(s))
			{
				keyWord.add(keyWord.size(), s);
				addKeyword(s, url);
				System.out.println("找到内容：" + s);
			}
		}
	}
	
	
	private synchronized void addReport(String url, String str) {
		synchronized (this)
		{
			try {
					PrintWriter pwReport = new PrintWriter(new FileOutputStream("report.txt"));
					String tmp = "Url：" + url + " "+ str + "\r\n";
					urlreport = urlreport + tmp;
					pwReport.print(urlreport);
					pwReport.close();
			} catch (Exception e) {
				System.out.println("生成报告文件失败!");
			}
		}
	}
	private synchronized void addKeyword(String str, String url){
		synchronized (this)
		{
			try {
					PrintWriter pw = new PrintWriter(new FileOutputStream("result.txt"));
					String tmp = str + "\r\n";
					keywordreport = keywordreport + tmp;
					pw.print(keywordreport);
					tmp = "Url:" + url + "\r\n";
					keywordreport = keywordreport + tmp;
					pw.print(keywordreport);
					pw.close();
			} catch (Exception e) {
				System.out.println("生成报告文件失败!");
			}
		}
	}
	
	
	public class myThread implements Runnable{
		private WebCrawler wc;
		private myThread(WebCrawler wc) {
			this.wc = wc;
		}
		@Override
		public void run() {
			while(wc.hasNextUrl()) 
			{
				try {
					wc.connectUrl();
				}catch(Exception e){
					System.out.println("读取网页失败");
					System.out.print("");
				}
			}
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		WebCrawler wc = new WebCrawler();
		//爬行初始URL
		wc.connectStartingUrl();
		//从待爬行URL队列中选取URL进行爬行
	}
}
