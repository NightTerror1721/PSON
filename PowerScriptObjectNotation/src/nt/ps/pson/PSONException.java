/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nt.ps.pson;

import nt.ps.exception.PSException;

/**
 *
 * @author Asus
 */
public class PSONException extends PSException
{
    private final int line;
    
    public PSONException(String message, int line)
    {
        super(message);
        this.line = line;
    }
    
    public PSONException(Throwable cause, int line)
    {
        super(cause);
        this.line = line;
    }
    
    public PSONException(String message, Throwable cause, int line)
    {
        super(message, cause);
        this.line = line;
    }
    
    public final int getSourceLine() { return line; }
}
