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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Objects;
import nt.ps.lang.LangUtils.ProtoObject;
import nt.ps.compiler.parser.Literal;
import nt.ps.lang.PSArray;
import nt.ps.lang.PSNumber.PSDouble;
import nt.ps.lang.PSNumber.PSLong;
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
    private final char[] buffer;
    private int index;
    private int max;
    private int line;
    private char lastChar = EOF;
    
    private static final char EOF = Character.MAX_VALUE;
    private static final char EOL = '\n';
    private static final char NAME_VALUE_SEPARATOR = ':';
    private static final char PROPERTY_SEPARATOR = ',';
    private static final char OPEN_OBJECT = '{';
    private static final char CLOSE_OBJECT = '}';
    private static final char STRING_DELIMITER_A = '\'';
    private static final char STRING_DELIMITER_B = '\"';
    private static final char OPEN_ARRAY = '[';
    private static final char CLOSE_ARRAY = ']';
    
    public PSONReader(InputStream is, int bufferLen)
    {
        if(bufferLen < 1)
            throw new IllegalArgumentException("bufferLen cannot be less than 1");
        source = new InputStreamReader(is);
        buffer = new char[bufferLen];
        max = index = 0;
    }
    
    public PSONReader(Reader reader, int bufferLen)
    {
        if(bufferLen < 1)
            throw new IllegalArgumentException("bufferLen cannot be less than 1");
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
                    return lastChar = EOF;
                index = 0;
            }
            char c = buffer[index++];
            switch(c)
            {
                case '\r': break;
                case EOL:
                    line++;
                default: return lastChar = c;
            }
        }
    }
    
    private char seek(boolean checkLastChar, char character) throws IOException, PSONException
    {
        int currentLine = line;
        if(checkLastChar && lastChar == character)
            return lastChar;
        for(char c = read();; c = read())
        {
            if(c == character)
                return c;
            if(c == EOF)
                throw new PSONException("Unexpected End of File", currentLine);
        }
    }
    
    private char seek(boolean checkLastChar, char character0, char character1) throws IOException, PSONException
    {
        int currentLine = line;
        if(checkLastChar && (lastChar == character0 || lastChar == character1))
            return lastChar;
        for(char c = read();; c = read())
        {
            if(c == character0 || c == character1)
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
    
    private String readPropertyName(boolean useLastChar, boolean first) throws IOException, PSONException
    {
        int currentLine = line;
        char c = useLastChar ? lastChar : readIgnoreSpaces();
        switch(c)
        {
            case EOF:
                if(first)
                    return null;
                throw new PSONException("Unexpected End of File", currentLine);
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
            case CLOSE_OBJECT:
                if(first)
                    return null;
            case NAME_VALUE_SEPARATOR:
            case OPEN_OBJECT:
            case OPEN_ARRAY:
            case CLOSE_ARRAY:
            case PROPERTY_SEPARATOR:
                throw new PSONException("Invalid name Character: " + c, currentLine);
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
                        case OPEN_ARRAY:
                        case CLOSE_ARRAY:
                        case PROPERTY_SEPARATOR:
                            throw new PSONException("Invalid name Character: " + c, currentLine);
                        default: sb.append(c);
                    }
                }
            }
        }
    }
    
    private PSValue readPropertyValue(boolean first) throws IOException, PSONException
    {
        int currentLine = line;
        char c = readIgnoreSpaces();
        switch(c)
        {
            case OPEN_OBJECT: return readObject(CLOSE_OBJECT, false);
            case OPEN_ARRAY: return readArray();
            case STRING_DELIMITER_A: return new PSString(readStringLiteral(STRING_DELIMITER_A));
            case STRING_DELIMITER_B: return new PSString(readStringLiteral(STRING_DELIMITER_B));
            case CLOSE_ARRAY:
                if(first)
                    return null;
            case NAME_VALUE_SEPARATOR:
            case CLOSE_OBJECT:
            case PROPERTY_SEPARATOR:
                throw new PSONException("Invalid name Character: " + c, currentLine);
        }
        
        StringBuilder sb = new StringBuilder(16);
        sb.append(c);
        for(c = read();; c = read())
        {
            switch(c)
            {
                case ' ':
                case '\t':
                case EOL:
                case EOF:
                case CLOSE_OBJECT:
                case CLOSE_ARRAY:
                case PROPERTY_SEPARATOR:
                    return decodeValue(sb.toString());
                case OPEN_OBJECT:
                case STRING_DELIMITER_A:
                case STRING_DELIMITER_B:
                case NAME_VALUE_SEPARATOR:
                    throw new PSONException("Invalid name Character: " + c, currentLine);
                default: sb.append(c); break;
            }
        }
    }
    
    private static PSValue decodeValue(String text)
    {
        switch(text)
        {
            case "undefined": return PSValue.UNDEFINED;
            case "null": return PSValue.NULL;
            case "true": return PSValue.TRUE;
            case "false": return PSValue.FALSE;
            case "-1": return PSValue.MINUSONE;
            case "0": return PSValue.ZERO;
            case "1": return PSValue.ONE;
            case "NaN": return Literal.NAN.getValue();
            case "Infinity": return Literal.INFINITY.getValue();
            case "-Infinity": return Literal.NEGATIVE_INFINITY.getValue();
        }
        
        try { return new PSLong(Long.decode(text)); } catch(NumberFormatException ex) {}
        try { return new PSDouble(Double.parseDouble(text)); } catch(NumberFormatException ex) {}
        
        return new PSString(text);
    }
    
    private PSArray readArray() throws IOException, PSONException
    {
        boolean first = true;
        ArrayList<PSValue> array = new ArrayList<>(8);
        for(;;)
        {
            PSValue value = readPropertyValue(first);
            if(value == null)
            {
                if(first)
                    return new PSArray();
                throw new IllegalStateException();
            }
            if(first)
                first = false;
            array.add(value);
            char end = seek(true, PROPERTY_SEPARATOR, CLOSE_ARRAY);
            if(end == CLOSE_ARRAY)
                return new PSArray(array);
        }
    }
    
    private PSObject readObject(char endChar, boolean useLastChar) throws IOException, PSONException
    {
        ProtoObject object = new ProtoObject();
        int count = 0;
        for(;;)
        {
            boolean first = count++ == 0;
            String name = readPropertyName(first && useLastChar, first);
            if(name == null)
            {
                if(first)
                    return object.build(false);
                throw new IllegalStateException();
            }
            seek(true, NAME_VALUE_SEPARATOR);
            PSValue value = readPropertyValue(false);
            object.put(name, value);
            char end = seek(true, PROPERTY_SEPARATOR, endChar);
            if(end == endChar)
                return object.build(false);
        }
    }
    
    public final PSObject readObject() throws IOException, PSONException
    {
        char first = readIgnoreSpaces();
        return first == OPEN_OBJECT
                ? readObject(CLOSE_OBJECT, false)
                : readObject(EOF, true);
    }
    
    public final <T extends PSONUserdataReader> T readUserdata(T object) throws IOException, PSONException
    {
        boolean useLastChar = readIgnoreSpaces() != OPEN_OBJECT;
        char endChar = useLastChar ? CLOSE_OBJECT : EOF;
        int count = 0;
        for(;;)
        {
            boolean first = count++ == 0;
            String name = readPropertyName(first && useLastChar, first);
            if(name == null)
            {
                if(first)
                    return object;
                throw new IllegalStateException();
            }
            seek(true, NAME_VALUE_SEPARATOR);
            PSValue value = readPropertyValue(false);
            object.readPSONProperty(name, value);
            char end = seek(true, PROPERTY_SEPARATOR, endChar);
            if(end == endChar)
                return object;
        }
    }
    
    public final <T extends PSONUserdataReader> T readUserdata(Class<T> objectClass) throws IllegalStateException, IOException, PSONException
    {
        try
        {
            Constructor<T> cns = objectClass.getDeclaredConstructor();
            T instance = cns.newInstance();
            return readUserdata(instance);
        }
        catch(IOException | IllegalAccessException | IllegalArgumentException |
                InstantiationException | NoSuchMethodException | SecurityException |
                InvocationTargetException | PSONException ex)
        {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public final void close() throws IOException
    {
        source.close();
    }
}
