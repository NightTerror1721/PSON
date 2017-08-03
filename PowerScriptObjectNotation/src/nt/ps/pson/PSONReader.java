/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nt.ps.pson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import nt.ps.compiler.LangUtils.ProtoObject;
import nt.ps.lang.PSObject;
import nt.ps.lang.PSString;
import nt.ps.lang.PSValue;

/**
 *
 * @author Asus
 */
public final class PSONReader implements AutoCloseable
{
    private final Reader source;
    private char[] buffer;
    private int index;
    private int max;
    private int line;
    
    private static final char EOF = Character.MAX_VALUE;
    private static final char EOL = '\n';
    private static final char NAME_VALUE_SEPARATOR = ':';
    private static final char PROPERTY_SEPARATOR = ',';
    private static final char OPEN_OBJECT = '{';
    private static final char CLOSE_OBJECT = '}';
    private static final char STRING_DELIMITER_A = '\'';
    private static final char STRING_DELIMITER_B = '\"';
    
    public PSONReader(InputStream is, int bufferLen)
    {
        source = new InputStreamReader(is);
        buffer = new char[bufferLen];
        max = index = 0;
    }
    
    public PSONReader(Reader reader, int bufferLen)
    {
        source = Objects.requireNonNull(reader);
        buffer = new char[bufferLen];
        max = index = 0;
    }
    
    private char read() throws IOException
    {
        for(;;)
        {
            if(index >= max)
            {
                max = source.read(buffer, 0, buffer.length);
                if(max <= 0)
                    return EOF;
                index = 0;
            }
            char c = buffer[index++];
            switch(c)
            {
                case '\r': break;
                case EOL:
                    line++;
                default: return c;
            }
        }
    }
    
    private char seek(char character) throws IOException, PSONException
    {
        int currentLine = line;
        for(char c = read();; c = read())
        {
            if(c == character)
                return c;
            if(c == EOF)
                throw new PSONException("Unexpected End of File", currentLine);
        }
    }
    
    private char seek(char... characters) throws IOException, PSONException
    {
        int currentLine = line;
        for(char c = read();; c = read())
        {
            for(char character : characters)
                if(c == character)
                    return c;
            if(c == EOF)
                throw new PSONException("Unexpected End of File", currentLine);
        }
    }
    
    private char readIgnoreSpaces() throws IOException
    {
        for(;;)
        {
            char c = read();
            switch(c)
            {
                case EOL:
                case '\t':
                case '\r':
                case ' ':
                    continue;
                case EOF:
                    return EOF;
            }
            return c;
        }
    }
    
    private String readStringLiteral(char endChar) throws IOException, PSONException
    {
        int currentLine = line;
        StringBuilder sb = new StringBuilder(16);
        for(char c = read(); c != EOF; c = read())
        {
            if(c == endChar)
                return sb.toString();
            if(c == '\\')
            {
                c = read();
                switch(c)
                {
                    case EOF: throw new PSONException("Unexpected End of File", currentLine);
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '0': sb.append('\u0000'); break;
                    case STRING_DELIMITER_A: sb.append(STRING_DELIMITER_A); break;
                    case STRING_DELIMITER_B: sb.append(STRING_DELIMITER_B); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(c); break;
                }
            }
            else sb.append(c);
        }
        throw new PSONException("Unexpected End of File", currentLine);
    }
    
    public final String readPropertyName() throws IOException, PSONException
    {
        int currentLine = line;
        char c = readIgnoreSpaces();
        switch(c)
        {
            case EOF: throw new PSONException("Unexpected End of File", currentLine);
            case STRING_DELIMITER_A: {
                String name = readStringLiteral(STRING_DELIMITER_A); 
                //seek(NAME_VALUE_SEPARATOR);
                return name;
            }
            case STRING_DELIMITER_B: {
                String name = readStringLiteral(STRING_DELIMITER_B);
                //seek(NAME_VALUE_SEPARATOR);
                return name;
            }
            default: {
                StringBuilder sb = new StringBuilder(16);
                sb.append(c);
                for(c = read();; c = read())
                {
                    switch(c)
                    {
                        case EOF:
                        case EOL:
                        case ' ':
                        case '\t':
                            //seek(NAME_VALUE_SEPARATOR);
                            return sb.toString();
                        case NAME_VALUE_SEPARATOR: return sb.toString();
                        case OPEN_OBJECT:
                        case CLOSE_OBJECT:
                        case PROPERTY_SEPARATOR:
                            throw new PSONException("Invalid name Character: " + c, currentLine);
                        default: sb.append(c);
                    }
                }
            }
        }
    }
    
    public final PSValue readPropertyValue() throws IOException, PSONException
    {
        char c = readIgnoreSpaces();
        switch(c)
        {
            case OPEN_OBJECT: return readObject(CLOSE_OBJECT);
            case STRING_DELIMITER_A: return new PSString(readStringLiteral(STRING_DELIMITER_A));
            case STRING_DELIMITER_B: return new PSString(readStringLiteral(STRING_DELIMITER_B));
        }
        
    }
    
    private PSObject readObject(char endChar) throws IOException, PSONException
    {
        ProtoObject object = new ProtoObject();
        for(;;)
        {
            String name = readPropertyName();
            seek(NAME_VALUE_SEPARATOR);
            PSValue value = readPropertyValue();
            object.put(name, value);
            char end = seek(PROPERTY_SEPARATOR, endChar);
            if(end == endChar)
                return object.build(false);
        }
    }
    
    public final PSObject readObject() throws IOException, PSONException { return readObject(EOF); }

    @Override
    public final void close() throws Exception
    {
        source.close();
    }
}
