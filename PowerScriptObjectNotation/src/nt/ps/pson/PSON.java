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
import nt.ps.lang.PSObject;

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
}
