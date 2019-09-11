package com.tivit.api.agent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class MetricsControllerTransformer implements ClassFileTransformer {
	
    @Override
    public byte[] transform(
      ClassLoader loader, 
      String className, 
      Class<?> classBeingRedefined, 
      ProtectionDomain protectionDomain, 
      byte[] classfileBuffer) {
        byte[] byteCode = classfileBuffer;
        String finalTargetClassName = this.getClass().getCanonicalName()
          .replaceAll("\\.", "/"); 
        if (!className.equals(finalTargetClassName)) {
            return byteCode;
        }
 
        if (className.equals(finalTargetClassName) 
              && loader.equals(this.getClass().getClassLoader())) {
  
            System.out.println("[Agent] Transforming class MyAtm");
            try {
                ClassPool cp = ClassPool.getDefault();
                CtClass cc = cp.get(this.getClass().getCanonicalName());
                CtMethod m = cc.getDeclaredMethod("doMetrics");
                m.addLocalVariable(
                  "startTime", CtClass.longType);
                m.insertBefore(
                  "startTime = System.currentTimeMillis();");
 
                StringBuilder endBlock = new StringBuilder();
 
                m.addLocalVariable("endTime", CtClass.longType);
                m.addLocalVariable("opTime", CtClass.longType);
                endBlock.append(
                  "endTime = System.currentTimeMillis();");
                endBlock.append(
                  "opTime = (endTime-startTime)/1000;");
 
                endBlock.append(
                  "LOGGER.info(\"[Application] Withdrawal operation completed in:" +
                                "\" + opTime + \" seconds!\");");
 
                m.insertAfter(endBlock.toString());
 
                byteCode = cc.toBytecode();
                cc.detach();
            } catch (NotFoundException | CannotCompileException | IOException e) {
                System.out.println("Exception" + e);
            }
        }
        return byteCode;
    }
}