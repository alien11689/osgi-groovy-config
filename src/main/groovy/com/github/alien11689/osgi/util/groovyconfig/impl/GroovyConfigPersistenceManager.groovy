package com.github.alien11689.osgi.util.groovyconfig.impl

import groovy.transform.CompileStatic
import org.apache.felix.cm.PersistenceManager

@CompileStatic
class GroovyConfigPersistenceManager implements PersistenceManager {

    private static final File configDir = new File(System.getProperty('karaf.etc'))

    @Override
    boolean exists(String pid) {
        return getFile(pid).exists()
    }

    @Override
    Dictionary load(String pid) throws IOException {
        println("Loading $pid")
        GroovyShell gs = new GroovyShell()
        ConfigObject co = new ConfigSlurper().parse(gs.parse(getFile(pid)))
        Hashtable dictionary = new Hashtable(co.toProperties() as Map)
        dictionary.put('service.pid', pid)
        return dictionary
    }

    @Override
    Enumeration getDictionaries() throws IOException {
        println("Get dictionaries")
        List<Dictionary> dictionaries = configDir.list { dir, name -> name ==~ /.+\.groovy$/ }
            .collect { String name -> name.split(/\./)[0..-2].join('.') }
            .collect { String pid -> load(pid) }
        return new DictionaryEnumeration(dictionaries)
    }

    @Override
    void store(String pid, Dictionary dictionary) throws IOException {
        println("Store $pid")
        Properties properties = new Properties()
        Enumeration keys = dictionary.keys()
        while (keys.hasMoreElements()) {
            String k = keys.nextElement()
            if (k == 'service.pid') {
                continue
            }
            properties[k] = dictionary.get(k)
        }
        new ConfigSlurper().parse(properties).writeTo(new FileWriter(getFile(pid)))
        println('ok')
    }

    @Override
    void delete(String pid) throws IOException {
        println("Delete $pid")
        File file = getFile(pid)
        if (file.exists()) {
            file.delete()
        }
    }

    private static String createFileName(String pid) {
        return "${pid}.groovy"
    }

    private static File getFile(String pid) {
        return new File(configDir, createFileName(pid))
    }

    class DictionaryEnumeration implements Enumeration {
        private final Stack<Dictionary> stack

        DictionaryEnumeration(List<Dictionary> dictionaries) {
            this.stack = new Stack<>()
            dictionaries.reverse().each {
                stack.push(it)
            }
        }

        @Override
        boolean hasMoreElements() {
            return !stack.empty()
        }

        @Override
        Object nextElement() {
            if (hasMoreElements()) {
                return stack.pop()
            }
            throw new NoSuchElementException()
        }
    }
}
