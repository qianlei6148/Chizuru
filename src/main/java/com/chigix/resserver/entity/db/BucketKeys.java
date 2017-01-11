package com.chigix.resserver.entity.db;

import org.mapdb.DB;
import org.mapdb.Serializer;

/**
 * Primary Key is performed by bucket name.
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public abstract class BucketKeys {

    public static final String CREATED_VALUE = "BUCKET_DB:VALUE:creation";

    public static void updateDBScheme(DB db) {
        if (!db.exists(CREATED_VALUE)) {
            db.hashMap(CREATED_VALUE, Serializer.STRING_ASCII, Serializer.STRING_ASCII).create();
        }
        db.commit();
    }

}
