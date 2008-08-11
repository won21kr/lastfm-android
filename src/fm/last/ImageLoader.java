package fm.last;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class ImageLoader implements Runnable
{
	boolean m_cachingEnabled = false; 
	HashMap<URL, Bitmap> m_bitmapCache = new HashMap<URL, Bitmap>();
	Thread m_thread = null;
	HashMap<ImageView, URL> m_imageMap = new HashMap<ImageView, URL>();
	Activity m_activity;
	
	public ImageLoader(Activity activity)
	{
		m_activity = activity;
	}
	
	public ImageLoader( Activity activity, boolean cachingEnabled )
	{
		this(activity);
		m_cachingEnabled = cachingEnabled;
	}
	
	public void loadImage( ImageView view, URL url )
	{
		if(m_cachingEnabled && m_bitmapCache.containsKey(url))
		{
			view.setImageBitmap(m_bitmapCache.get(url));
			return;
		}
		
		view.setImageResource( android.R.drawable.screen_background_dark );
		
		synchronized(m_imageMap)
		{
			m_imageMap.put(view, url);
		}
		
		if(m_thread == null || !m_thread.isAlive())
		{
			start();
		}
	}
	
	public void start()
	{
		m_thread = new Thread(this);
		m_thread.start();
	}
	
	private Bitmap downloadImage( URL url )
	{
		Bitmap bm = null;
		try 
		{
			URLConnection conn = url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();

			Log.i("Begining download of event image: " + url);
			bm = BitmapFactory.decodeStream(is);
			Log.i("Completed download of event image");

			is.close();
		}
		catch (Exception e) 
		{
			Log.e("Error downloading event art");
			
		}

		return bm;
	}
	
	public void run()
	{
		Log.i("ImageLoader Thread running");
		while( !m_imageMap.isEmpty() )
		{
			Iterator<Map.Entry<ImageView,URL>> it;
			it = m_imageMap.entrySet().iterator();
			
			Entry<ImageView, URL> pair;
			pair = (Map.Entry<ImageView, URL>)it.next();

			URL url = pair.getValue();
			final Bitmap bmp = downloadImage(url);

			if(m_cachingEnabled)
				m_bitmapCache.put(url, bmp);					

			final ImageView iv = pair.getKey();
			
			m_activity.runOnUiThread(new Runnable() {
				public void run()
				{
					iv.setImageBitmap(bmp);
				}
			});

			synchronized (m_imageMap) {
				m_imageMap.remove(pair.getKey());						
			}
		}
	}
}
