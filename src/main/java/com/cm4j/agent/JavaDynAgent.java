package com.cm4j.agent;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class JavaDynAgent {



    public static void agentmain(String args, Instrumentation inst) throws IOException, ClassNotFoundException, UnmodifiableClassException {
        LinkedHashMap<String, LinkedHashSet<Class<?>>> redefineMap = Maps.newLinkedHashMap();
        // 1.整理需要重定义的类
        List<String> classArr = Arrays.stream(args.split(",")).collect(Collectors.toList());
        List<ClassDefinition> classDefList = new ArrayList<ClassDefinition>();
        for (String className : classArr) {
                Class<?> c = Class.forName(className);
                String classLocation = c.getProtectionDomain().getCodeSource().getLocation().getPath();
                LinkedHashSet<Class<?>> classSet = redefineMap.computeIfAbsent(classLocation,
                        k -> Sets.newLinkedHashSet());
                classSet.add(c);
        }
        if (!redefineMap.isEmpty()) {
            for (Map.Entry<String, LinkedHashSet<Class<?>>> entry : redefineMap.entrySet()) {
                String classLocation = entry.getKey();
                if (classLocation.endsWith(".jar")) {
                    try (JarFile jf = new JarFile(classLocation)) {
                        for (Class<?> cls : entry.getValue()) {
                            String clazz = cls.getName().replace('.', '/') + ".class";
                            JarEntry je = jf.getJarEntry(clazz);
                            if (je != null) {
                                try (InputStream stream = jf.getInputStream(je)) {
                                    byte[] data = IOUtils.toByteArray(stream);
                                    classDefList.add(new ClassDefinition(cls, data));
                                }
                            } else {
                                throw new IOException("JarEntry " + clazz + " not found");
                            }
                        }
                    }
                } else {
                    File file;
                    for (Class<?> cls : entry.getValue()) {
                        String clazz = cls.getName().replace('.', '/') + ".class";
                        file = new File(classLocation, clazz);
                        byte[] data = FileUtils.readFileToByteArray(file);
                        classDefList.add(new ClassDefinition(cls, data));
                    }
                }
            }
            // 2.redefine
            inst.redefineClasses(classDefList.toArray(new ClassDefinition[0]));
        }
    }





}
