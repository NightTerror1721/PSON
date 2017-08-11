/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nt.ps.pson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import nt.ps.lang.PSObject;
import nt.ps.lang.PSObject.PropertyEntry;

/**
 *
 * @author Asus
 */
public final class PSON
{
    public static final int DEFAULT_BUFFER_LENGTH = 8192;
    
    private PSON() {}
    
    public static final PSObject read(InputStream input, int bufferLength) throws IOException, PSONException
    {
        return new PSONReader(input, bufferLength).readObject();
    }
    public static final PSObject read(InputStream input) throws IOException, PSONException { return read(input, DEFAULT_BUFFER_LENGTH); }
    
    public static final PSObject read(Reader reader, int bufferLength) throws IOException, PSONException
    {
        return new PSONReader(reader, bufferLength).readObject();
    }
    public static final PSObject read(Reader reader) throws IOException, PSONException { return read(reader, DEFAULT_BUFFER_LENGTH); }
    
    public static final PSObject read(File file, int bufferLength) throws IOException, PSONException
    {
        try(FileInputStream fis = new FileInputStream(file)) { return read(fis, bufferLength); }
    }
    public static final PSObject read(File file) throws IOException, PSONException
    {
        try(FileInputStream fis = new FileInputStream(file)) { return read(fis, DEFAULT_BUFFER_LENGTH); }
    }
    
    
    public static final void write(PSObject object, boolean wrapped, OutputStream output, int bufferLength) throws IOException
    {
        PSONWriter w = new PSONWriter(output, bufferLength);
        w.writeObject(object, wrapped);
        w.flush();
    }
    public static final void write(PSObject object, boolean wrapped, OutputStream output) throws IOException { write(object, wrapped, output, DEFAULT_BUFFER_LENGTH); }
    
    public static final void write(PSObject object, boolean wrapped, Writer writer, int bufferLength) throws IOException
    {
        PSONWriter w = new PSONWriter(writer, bufferLength);
        w.writeObject(object, wrapped);
        w.flush();
    }
    public static final void write(PSObject object, boolean wrapped, Writer writer) throws IOException { write(object, wrapped, writer, DEFAULT_BUFFER_LENGTH); }
    
    public static final void write(PSObject object, boolean wrapped, File file, int bufferLength) throws IOException
    {
        try(FileOutputStream fos = new FileOutputStream(file)) { write(object, wrapped, fos, bufferLength); }
    }
    
    public static final void write(PSObject object, boolean wrapped, File file) throws IOException
    {
        try(FileOutputStream fos = new FileOutputStream(file)) { write(object, wrapped, fos, DEFAULT_BUFFER_LENGTH); }
    }
    
    
    
    public static final <T extends PSONUserdataReader> T read(T object, InputStream input, int bufferLength) throws IOException, PSONException
    {
        return new PSONReader(input, bufferLength).readUserdata(object);
    }
    public static final <T extends PSONUserdataReader> T read(T object, InputStream input) throws IOException, PSONException { return read(object, input, DEFAULT_BUFFER_LENGTH); }
    
    public static final <T extends PSONUserdataReader> T read(Class<T> objectClass, InputStream input, int bufferLength) throws IllegalStateException, IOException, PSONException
    {
        return new PSONReader(input, bufferLength).readUserdata(objectClass);
    }
    public static final <T extends PSONUserdataReader> T read(Class<T> objectClass, InputStream input) throws IllegalStateException, IOException, PSONException
    {
        return read(objectClass, input, DEFAULT_BUFFER_LENGTH);
    }
    
    public static final <T extends PSONUserdataReader> T read(T object, Reader reader, int bufferLength) throws IOException, PSONException
    {
        return new PSONReader(reader, bufferLength).readUserdata(object);
    }
    public static final <T extends PSONUserdataReader> T read(T object, Reader reader) throws IOException, PSONException { return read(object, reader, DEFAULT_BUFFER_LENGTH); }
    
    public static final <T extends PSONUserdataReader> T read(Class<T> objectClass, Reader reader, int bufferLength) throws IllegalStateException, IOException, PSONException
    {
        return new PSONReader(reader, bufferLength).readUserdata(objectClass);
    }
    public static final <T extends PSONUserdataReader> T read(Class<T> objectClass, Reader reader) throws IllegalStateException, IOException, PSONException
    {
        return read(objectClass, reader, DEFAULT_BUFFER_LENGTH);
    }
    
    public static final <T extends PSONUserdataReader> T read(T object, File file, int bufferLength) throws IOException, PSONException
    {
        try(FileInputStream fis = new FileInputStream(file)) { return read(object, fis, bufferLength); }
    }
    public static final <T extends PSONUserdataReader> T read(T object, File file) throws IOException, PSONException { return read(object, file, DEFAULT_BUFFER_LENGTH); }
    
    public static final <T extends PSONUserdataReader> T read(Class<T> objectClass, File file, int bufferLength) throws IllegalStateException, IOException, PSONException
    {
        try(FileInputStream fis = new FileInputStream(file)) { return read(objectClass, fis, bufferLength); }
    }
    public static final <T extends PSONUserdataReader> T read(Class<T> objectClass, File file) throws IllegalStateException, IOException, PSONException
    {
        return read(objectClass, file, DEFAULT_BUFFER_LENGTH);
    }
    
    
    
    public static final void write(PSONUserdataWriter object, boolean wrapped, OutputStream output, int bufferLength) throws IOException
    {
        PSONWriter w = new PSONWriter(output, bufferLength);
        w.writeUserdata(object, wrapped);
        w.flush();
    }
    public static final void write(PSONUserdataWriter object, boolean wrapped, OutputStream output) throws IOException { write(object, wrapped, output, DEFAULT_BUFFER_LENGTH); }
    
    public static final void write(PSONUserdataWriter object, boolean wrapped, Writer writer, int bufferLength) throws IOException
    {
        PSONWriter w = new PSONWriter(writer, bufferLength);
        w.writeUserdata(object, wrapped);
        w.flush();
    }
    public static final void write(PSONUserdataWriter object, boolean wrapped, Writer writer) throws IOException { write(object, wrapped, writer, DEFAULT_BUFFER_LENGTH); }
    
    public static final void write(PSONUserdataWriter object, boolean wrapped, File file, int bufferLength) throws IOException
    {
        try(FileOutputStream fos = new FileOutputStream(file)) { write(object, wrapped, fos, bufferLength); }
    }
    public static final void write(PSONUserdataWriter object, boolean wrapped, File file) throws IOException { write(object, wrapped, file, DEFAULT_BUFFER_LENGTH); }
    
    
    
    public static final <T extends PSONUserdataReader> T read(T object, PSObject inputObject) throws IOException
    {
        for(PropertyEntry p : inputObject.properties())
            object.readPSONProperty(p.getName(), p.getValue());
        return object;
    }
    
    public static final <T extends PSONUserdataReader> T read(Class<T> objectClass, PSObject inputObject)
    {
        try
        {
            Constructor<T> cns = objectClass.getDeclaredConstructor();
            T instance = cns.newInstance();
            return read(instance, inputObject);
        }
        catch(IOException | IllegalAccessException | IllegalArgumentException |
                InstantiationException | NoSuchMethodException | SecurityException |
                InvocationTargetException ex)
        {
            throw new IllegalStateException(ex);
        }
    }
}
