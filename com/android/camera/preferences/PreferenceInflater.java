package com.android.camera.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;

public class PreferenceInflater {
    private static final Class<?>[] CTOR_SIGNATURE = new Class[]{Context.class, AttributeSet.class};
    private static final String PACKAGE_NAME = PreferenceInflater.class.getPackage().getName();
    private static final HashMap<String, Constructor<?>> sConstructorMap = new HashMap();
    private Context mContext;

    public PreferenceInflater(Context context) {
        this.mContext = context;
    }

    private CameraPreference inflate(XmlPullParser xmlPullParser) {
        AttributeSet asAttributeSet = Xml.asAttributeSet(xmlPullParser);
        ArrayList arrayList = new ArrayList();
        Object[] objArr = new Object[]{this.mContext, asAttributeSet};
        try {
            int next = xmlPullParser.next();
            while (next != 1) {
                if (next == 2) {
                    CameraPreference newPreference = newPreference(xmlPullParser.getName(), objArr);
                    int depth = xmlPullParser.getDepth();
                    if (depth > arrayList.size()) {
                        arrayList.add(newPreference);
                    } else {
                        arrayList.set(depth - 1, newPreference);
                    }
                    if (depth > 1) {
                        ((PreferenceGroup) arrayList.get(depth - 2)).addChild(newPreference);
                    }
                }
                next = xmlPullParser.next();
            }
            if (arrayList.size() != 0) {
                return (CameraPreference) arrayList.get(0);
            }
            throw new InflateException("No root element found");
        } catch (Throwable e) {
            throw new InflateException(e);
        } catch (Throwable e2) {
            throw new InflateException(xmlPullParser.getPositionDescription(), e2);
        }
    }

    private CameraPreference newPreference(String str, Object[] objArr) {
        String str2 = PACKAGE_NAME + "." + str;
        Constructor constructor = (Constructor) sConstructorMap.get(str2);
        if (constructor == null) {
            try {
                constructor = this.mContext.getClassLoader().loadClass(str2).getConstructor(CTOR_SIGNATURE);
                sConstructorMap.put(str2, constructor);
            } catch (Throwable e) {
                throw new InflateException("Error inflating class " + str2, e);
            } catch (Throwable e2) {
                throw new InflateException("No such class: " + str2, e2);
            } catch (Throwable e3) {
                throw new InflateException("While create instance of" + str2, e3);
            }
        }
        return (CameraPreference) constructor.newInstance(objArr);
    }

    public CameraPreference inflate(int i) {
        return inflate(this.mContext.getResources().getXml(i));
    }
}
