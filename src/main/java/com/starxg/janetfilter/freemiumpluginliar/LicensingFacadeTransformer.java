package com.starxg.janetfilter.freemiumpluginliar;

import com.janetfilter.core.plugin.MyTransformer;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.ByteArrayInputStream;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LicensingFacadeTransformer implements MyTransformer {

    private final Map<String, Long> expires = new ConcurrentHashMap<>(10);


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
        for (Map.Entry<String, Long> e : expires.entrySet()) {
            sb.append("if(\"").append(e.getKey()).append("\".equals($1))return \"eval:").append(e.getValue()).append("\";");
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
        this.expires.put(productCode, expires);
        return this;
    }
}
