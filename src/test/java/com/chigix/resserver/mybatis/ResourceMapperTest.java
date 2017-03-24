package com.chigix.resserver.mybatis;

import com.chigix.resserver.entity.error.NoSuchBucket;
import com.chigix.resserver.mybatis.bean.BucketBean;
import com.chigix.resserver.mybatis.bean.ChunkedResourceBean;
import com.chigix.resserver.mybatis.bean.ResourceExtension;
import com.chigix.resserver.mybatis.dto.ResourceDto;
import java.io.IOException;
import java.util.HashMap;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ResourceMapperTest {

    private SqlSession session;

    private ResourceMapper mapper;

    public ResourceMapperTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        session = TestUtils.setUpDatabase();
        session.update("com.chigix.resserver.mybatis.DbInitMapper.createBucketTable");
        session.update("com.chigix.resserver.mybatis.DbInitMapper.createResourceTable");
        mapper = session.getMapper(ResourceMapper.class);
    }

    @After
    public void tearDown() {
        session.update("com.chigix.resserver.mybatis.DbInitMapper.deleteBucketTable");
        session.update("com.chigix.resserver.mybatis.DbInitMapper.deleteResourceTable");
    }

    /**
     * Test of selectAllByBucketName method, of class ResourceMapper.
     *
     * @throws com.chigix.resserver.entity.error.NoSuchBucket
     */
    @Test
    public void testSelectAllByBucketName() throws NoSuchBucket {
        System.out.println("selectAllByBucketName");
        BucketBean bb = new BucketBean("TEST_BUCKET");
        HashMap<String, String> map = new HashMap<>();
        map.put("uuid", bb.getUuid());
        map.put("name", bb.getName());
        map.put("created_at", bb.getCreationTime().toString());
        session.insert("com.chigix.resserver.mybatis.BucketMapper.insert", map);
        ChunkedResourceBean r_1 = new ChunkedResourceBean("REC_1", ResourceExtension.hashKey(bb.getUuid(), "REC_1"));
        r_1.setBucket(bb);
        mapper.insert(new ResourceDto(r_1));
        ChunkedResourceBean r_2 = new ChunkedResourceBean("REC_2", ResourceExtension.hashKey(bb.getUuid(), "REC_2"));
        r_2.setBucket(bb);
        mapper.insert(new ResourceDto(r_2));
        ChunkedResourceBean r_3 = new ChunkedResourceBean("REC_3", ResourceExtension.hashKey(bb.getUuid(), "REC_3"));
        r_3.setBucket(bb);
        mapper.insert(new ResourceDto(r_3));
        assertEquals(3, mapper.selectAllByBucketName("TEST_BUCKET", 1000).size());
        assertEquals(2, mapper.selectAllByBucketName("TEST_BUCKET", 1000, r_2.getKeyHash()).size());
    }

    /**
     * Test of selectByKeyhash method, of class ResourceMapper.
     *
     * @throws com.chigix.resserver.entity.error.NoSuchBucket
     */
    @Test
    public void testSelectByKeyhash() throws NoSuchBucket {
        System.out.println("selectByKeyhash");
        BucketBean bb = new BucketBean("TEST_BUCKET");
        HashMap<String, String> map = new HashMap<>();
        map.put("uuid", bb.getUuid());
        map.put("name", bb.getName());
        map.put("created_at", bb.getCreationTime().toString());
        session.insert("com.chigix.resserver.mybatis.BucketMapper.insert", map);
        System.out.println(session.selectOne("com.chigix.resserver.mybatis.BucketMapper.selectByName", map).toString());
        ChunkedResourceBean r_1 = new ChunkedResourceBean("REC_1", ResourceExtension.hashKey(bb.getUuid(), "REC_1"));
        r_1.setBucket(bb);
        mapper.insert(new ResourceDto(r_1));
        ChunkedResourceBean r_2 = new ChunkedResourceBean("REC_2", ResourceExtension.hashKey(bb.getUuid(), "REC_2"));
        r_2.setBucket(bb);
        mapper.insert(new ResourceDto(r_2));
        ChunkedResourceBean r_3 = new ChunkedResourceBean("REC_3", ResourceExtension.hashKey(bb.getUuid(), "REC_3"));
        r_3.setBucket(bb);
        mapper.insert(new ResourceDto(r_3));
        assertEquals("REC_3", mapper.selectByKeyhash(r_3.getKeyHash()).get("key"));
    }

    /**
     * Test of selectByBucketName_Key method, of class ResourceMapper.
     *
     * @throws com.chigix.resserver.entity.error.NoSuchBucket
     */
    @Test
    public void testSelectByBucketName_Key() throws NoSuchBucket {
        System.out.println("selectByBucketName_Key");
        BucketBean bb = new BucketBean("TEST_BUCKET");
        HashMap<String, String> map = new HashMap<>();
        map.put("uuid", bb.getUuid());
        map.put("name", bb.getName());
        map.put("created_at", bb.getCreationTime().toString());
        session.insert("com.chigix.resserver.mybatis.BucketMapper.insert", map);
        System.out.println(session.selectOne("com.chigix.resserver.mybatis.BucketMapper.selectByName", map).toString());
        ChunkedResourceBean r_1 = new ChunkedResourceBean("REC_1", ResourceExtension.hashKey(bb.getUuid(), "REC_1"));
        r_1.setBucket(bb);
        mapper.insert(new ResourceDto(r_1));
        ChunkedResourceBean r_2 = new ChunkedResourceBean("REC_2", ResourceExtension.hashKey(bb.getUuid(), "REC_2"));
        r_2.setBucket(bb);
        mapper.insert(new ResourceDto(r_2));
        ChunkedResourceBean r_3 = new ChunkedResourceBean("REC_3", ResourceExtension.hashKey(bb.getUuid(), "REC_3"));
        r_3.setBucket(bb);
        mapper.insert(new ResourceDto(r_3));
        assertEquals(r_3.getKeyHash(), mapper.selectByBucketName_Key("TEST_BUCKET", "REC_3").get("keyhash"));
    }

    /**
     * Test of insert method, of class ResourceMapper.
     *
     * @throws java.lang.Exception
     * @throws com.chigix.resserver.entity.error.NoSuchBucket
     */
    @Test
    public void testInsert() throws Exception {
        System.out.println("insert");
        BucketBean bb = new BucketBean("TEST_BUCKET");
        ChunkedResourceBean r_chunked = new ChunkedResourceBean("TEST_RESOURCE_KEY", ResourceExtension.hashKey(bb.getUuid(), "TEST_RESOURCE_KEY"));
        r_chunked.setBucket(bb);
        assertEquals(1, mapper.insert(new ResourceDto(r_chunked)));
    }

    /**
     * Test of update method, of class ResourceMapper.
     *
     * @throws com.chigix.resserver.entity.error.NoSuchBucket
     */
    @Test
    public void testUpdate() throws NoSuchBucket {
        System.out.println("update");
        BucketBean bb = new BucketBean("TEST_BUCKET");
        ChunkedResourceBean r_chunked = new ChunkedResourceBean("TEST_RESOURCE_KEY", ResourceExtension.hashKey(bb.getUuid(), "TEST_RESOURCE_KEY"));
        r_chunked.setBucket(bb);
        mapper.insert(new ResourceDto(r_chunked));
        r_chunked.setSize("123123123");
        mapper.update(new ResourceDto(r_chunked));
        assertEquals("123123123", mapper.selectByKeyhash(r_chunked.getKeyHash()).get("size"));
    }

    /**
     * Test of delete method, of class ResourceMapper.
     *
     * @throws com.chigix.resserver.entity.error.NoSuchBucket
     */
    @Test
    public void testDelete() throws NoSuchBucket {
        System.out.println("delete");
        BucketBean bb = new BucketBean("TEST_BUCKET");
        HashMap<String, String> map = new HashMap<>();
        map.put("uuid", bb.getUuid());
        map.put("name", bb.getName());
        map.put("created_at", bb.getCreationTime().toString());
        session.insert("com.chigix.resserver.mybatis.BucketMapper.insert", map);
        ChunkedResourceBean r_chunked = new ChunkedResourceBean("TEST_RESOURCE_KEY", ResourceExtension.hashKey(bb.getUuid(), "TEST_RESOURCE_KEY"));
        r_chunked.setBucket(bb);
        mapper.insert(new ResourceDto(r_chunked));
        assertEquals(1, mapper.delete(r_chunked));
    }

    /**
     * Test of deleteByKeyhash method, of class ResourceMapper.
     *
     * @throws com.chigix.resserver.entity.error.NoSuchBucket
     */
    @Test
    public void testDeleteByKeyhash() throws NoSuchBucket {
        System.out.println("deleteByKeyhash");
        BucketBean bb = new BucketBean("TEST_BUCKET");
        ChunkedResourceBean r_chunked = new ChunkedResourceBean("TEST_RESOURCE_KEY", ResourceExtension.hashKey(bb.getUuid(), "TEST_RESOURCE_KEY"));
        r_chunked.setBucket(bb);
        mapper.insert(new ResourceDto(r_chunked));
        assertEquals(1, mapper.deleteByKeyhash(r_chunked.getKeyHash()));
    }

    /**
     * Test of merge method, of class ResourceMapper.
     *
     * @throws com.chigix.resserver.entity.error.NoSuchBucket
     */
    @Test
    public void testMerge() throws NoSuchBucket {
        System.out.println("merge");
        BucketBean bb = new BucketBean("TEST_BUCKET");
        ChunkedResourceBean r_chunked = new ChunkedResourceBean("TEST_RESOURCE_KEY",
                ResourceExtension.hashKey(bb.getUuid(), "TEST_RESOURCE_KEY"));
        r_chunked.setBucket(bb);
        assertEquals(1, mapper.merge(new ResourceDto(r_chunked)));
    }

}
