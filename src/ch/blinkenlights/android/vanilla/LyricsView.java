package ch.blinkenlights.android.vanilla;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alexwang on 2/20/16.
 */
public class LyricsView extends TextView {

    TreeMap<Integer, String> lyricsLines = new TreeMap<>();
    int currentMillis = 0;
    int totalLines = 8;
    int currentLine = 3;

    public LyricsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLyricsFilePathFromSongPath(String songPath) {
        Pattern pattern = Pattern.compile("^(.*)\\.(\\w+)$");
        Matcher matcher = pattern.matcher(songPath);
        if (matcher.matches()) {
            String lrcPath = matcher.group(1) + ".lrc";
            try {
                Reader reader = new FileReader(lrcPath);
                setLyrics(reader);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setLyrics(Reader reader) {
        lyricsLines = new TreeMap<>();
        BufferedReader br = new BufferedReader(reader);
        try {
            String line;
            while ((line = br.readLine()) != null) {
                Pattern p = Pattern.compile("^(\\[\\d+:\\d+\\.\\d+\\])+([^\\[]*)$");
                Matcher matcher = p.matcher(line);
                if (matcher.matches()) {
                    String content = matcher.group(2);

                    Pattern pTime = Pattern.compile(".*\\[(\\d+):(\\d+)\\.(\\d+)\\].*");
                    Matcher matcherTime = pTime.matcher(line);
                    while (matcherTime.find()) {
                        int min = Integer.parseInt(matcherTime.group(1));
                        int sec =  Integer.parseInt(matcherTime.group(2));
                        int millis = 10 * Integer.parseInt(matcherTime.group(3));

                        lyricsLines.put(millis + (sec + min * 60) * 1000, content);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set current time so that the current line points to the right lyrics line.
     * This method must be called from UI thread.
     *
     * @param currentMillis current time in millisecond
     */
    public void setCurrentMillis(int currentMillis) {
        if (lyricsLines.size() <= 0)
            return;

        int currentLineTime = lyricsLines.lastKey();
        for (Map.Entry<Integer, String> entry : lyricsLines.entrySet()) {
            int lineTime = entry.getKey();
            String content = entry.getValue();
            if (lineTime >= currentMillis) {
                currentLineTime = lineTime;
                break;
            }
        }

        this.currentMillis = currentLineTime;
        updateCenterLine();
    }

    protected void updateCenterLine() {
        int centerLine = totalLines / 2;
        int indexOfCenterLine = getIndexOfKey(lyricsLines, currentMillis);
        if (indexOfCenterLine < 0 || indexOfCenterLine > lyricsLines.size())
            return;

        StringBuilder htmlLyricsSb = new StringBuilder();
        for (int i = 0; i < totalLines; ++i) {
            Map.Entry<Integer, String> entry = getEntryByIndex(lyricsLines, indexOfCenterLine - centerLine + i);
            if (entry != null) {
                String lyrics = entry.getValue();
                if (centerLine == i)
                    htmlLyricsSb.append("<font color=\"red\">").append(lyrics).append("</font><br/>");
                else
                    htmlLyricsSb.append(lyrics).append("<br/>");
            }
            else {
                htmlLyricsSb.append("------------<br/>");
            }
        }

        setText(Html.fromHtml(htmlLyricsSb.toString()));
    }

    static int getIndexOfKey(TreeMap<Integer, String> map, int key) {
        int i = 0;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getKey() == key)
                return i;
            i++;
        }
        return -1;
    }
    static Map.Entry<Integer, String> getEntryByIndex(TreeMap<Integer, String> map, int index) {
        if (index >= map.size() || index < 0)
            return null;

        int i = 0;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (index == i)
                return entry;
            i++;
        }
        return null;
    }
}
