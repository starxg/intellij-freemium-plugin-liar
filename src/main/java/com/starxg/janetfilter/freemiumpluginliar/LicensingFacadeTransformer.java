package com.starxg.janetfilter.freemiumpluginliar;

import com.janetfilter.core.plugin.MyTransformer;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.ByteArrayInputStream;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LicensingFacadeTransformer implements MyTransformer {

    private final List<Lie> expires = new CopyOnWriteArrayList<>();


    public static final String KEY_PREFIX = "key:";
    public static final String STAMP_PREFIX = "stamp:";
    public static final String EVAL_PREFIX = "eval:";

    @Override
    public String getHookClassName() {
        return "com/intellij/ui/LicensingFacade";
    }


    @Override
    public byte[] transform(ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, String className, byte[] classBytes, int order) throws Exception {

        if (expires.isEmpty()) {
            return classBytes;
        }

        final ClassPool pool = ClassPool.getDefault();
        pool.appendSystemPath();
        final CtClass clazz = pool.makeClass(new ByteArrayInputStream(classBytes));

        final StringBuilder sb = new StringBuilder();
        for (Lie e : expires) {
            sb.append("if(\"").append(e.productCode).append("\".equals($1)){ return \"").append(e.prefix).append(e.expires).append("\";}");
        }

        clazz.getDeclaredMethod("getConfirmationStamp", new CtClass[]{pool.get("java.lang.String")})
                .insertBefore(sb.toString());

        classBytes = clazz.toBytecode();
        clazz.detach();

        return classBytes;
    }


    /**
     * 设置过期时间 ， 2099 年
     *
     * @param productCode 产品码
     */
    public LicensingFacadeTransformer lie(String productCode) {
        // 2099
        return lie(productCode, 4081698416000L);

    }

    /**
     * 设置过期时间
     *
     * @param productCode 产品码
     * @param expires     过期时间，单位时间戳
     */
    public LicensingFacadeTransformer lie(String productCode, long expires) {
        return lie(EVAL_PREFIX, productCode, String.valueOf(expires));
    }

    /**
     * 设置过期时间
     *
     * @param prefix      {@link #EVAL_PREFIX}、{@link #KEY_PREFIX}、{@link #STAMP_PREFIX}
     * @param productCode 产品码
     * @param expires     过期时间，单位时间戳
     */
    public LicensingFacadeTransformer lie(String prefix, String productCode, String expires) {
        final Lie lie = new Lie();
        lie.prefix = prefix;
        lie.productCode = productCode;
        lie.expires = expires;

        this.expires.add(lie);

        return this;
    }


    private static class Lie {
        private String prefix;
        private String productCode;
        private String expires;
    }
}
