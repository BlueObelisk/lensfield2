package org.lensfield.atom;

import nu.xom.Document;
import nu.xom.Serializer;
import org.apache.commons.codec.digest.DigestUtils;
import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;
import org.lensfield.api.LensfieldParameter;
import org.lensfield.api.io.MultiStreamOut;
import org.lensfield.api.io.StreamIn;
import org.lensfield.api.io.StreamOut;
import uk.ac.cam.ch.atomxom.feeds.DefaultFeedHandler;
import uk.ac.cam.ch.atomxom.feeds.FeedCache;
import uk.ac.cam.ch.atomxom.model.AtomEntry;

import java.io.File;
import java.io.IOException;

/**
 * @author sea36
 */
public class AtomFeed {

    @LensfieldInput
    private StreamIn in;

    @LensfieldOutput
    private MultiStreamOut out;

    @LensfieldParameter
    private String url;

    @LensfieldParameter(optional = true)
    private String cacheDir = ".lf/feedcache";

    @LensfieldParameter(optional = true)
    private String dirLevels = "1";

    public void run() throws Exception {
        FeedCache cache = new FeedCache();
        cache.setHandler(new Handler());
        cache.setCacheDir(new File(cacheDir));
        cache.poll(url);
    }

    private class Handler extends DefaultFeedHandler {

        private int dirs = Integer.parseInt(dirLevels);

        @Override
        public void nextEntry(AtomEntry entry) {
            try {
                StreamOut out = AtomFeed.this.out.next();
                try {
                    String md5 = DigestUtils.md5Hex(entry.getId()+"/"+entry.getUpdated().toString());
                    out.setParameter("*",md5);
                    out.setParameter("**", getDir(md5));
                    Document doc = new Document(entry.copy());
                    Serializer s = new Serializer(out);
                    s.write(doc);
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String getDir(String md5) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < dirs; i++) {
                s.append(md5.substring(2*i, (2*i)+2));
                s.append('/');
            }
            return s.toString();
        }

    }

}
