/**

Copyright (C) SYSTAP, LLC 2006-2007.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.bigdata.rdf.spo;

import org.openrdf.model.Statement;

import com.bigdata.rdf.inf.Justification;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.internal.IVUtility;
import com.bigdata.rdf.internal.SidIV;
import com.bigdata.rdf.model.BigdataResource;
import com.bigdata.rdf.model.BigdataStatement;
import com.bigdata.rdf.model.BigdataStatementImpl;
import com.bigdata.rdf.model.BigdataURI;
import com.bigdata.rdf.model.BigdataValue;
import com.bigdata.rdf.model.StatementEnum;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.rdf.store.IRawTripleStore;
import com.bigdata.relation.accesspath.IAccessPath;
import com.bigdata.relation.rule.IConstant;
import com.bigdata.relation.rule.IPredicate;
import com.bigdata.relation.rule.IVariableOrConstant;
import com.bigdata.util.Bits;

/**
 * Represents a triple, triple+SID, or quad. When used to represent a triple,
 * the statement identifier MAY be set on the triple after the object has been
 * instantiated. When used to represent a quad, the context position SHOULD be
 * treated as immutable and {@link #setStatementIdentifier(IV)} will reject
 * arguments if they can not be validated as statement identifiers (based on
 * their bit pattern).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class SPO implements ISPO {
    
	
    /** The internal value for the subject position. */
    public final IV s;

    /** The internal value for the predicate position. */
    public final IV p;
    
    /** The internal value for the object position. */
    public final IV o;

    /**
     * The context position or statement identifier (optional).
     * <p>
     * Note: this is not final since, for SIDs mode, we have to set it lazily
     * when adding an {@link SPO} to the database.
     */
    private IV c = null;

//    /**
//     * Statement type (inferred, explicit, or axiom).
//     */
//    private StatementEnum type;
//    
//    /**
//     * User flag
//     */
//    private boolean userFlag;
//    
//    /**
//     * Override flag used for downgrading statements during truth maintenance.
//     */
//    private transient boolean override = false;
//
//    /**
//     * Used to signify if and how this spo was modified. Used by change log.
//     */
//    private transient ModifiedEnum modified = ModifiedEnum.NONE;
//    
//    /**
//     * If sidable, we will lazily instantiate a sid when requested via {@link #c()},
//     * {@link #getStatementIdentifier()}, and {@link SPO#get(int)} with a 
//     * parameter of 3.  This should reduce heap pressure by only creating
//     * sids on-demand on an as-needed basis.
//     */
//    private boolean sidable = false;
    
	/**
	 * Bit flags used to represent statement type, user flag, override, 
	 * modified enum, and sidable flag.  Much more compact representation.
	 */
	private byte flags = 0;

	/**
	 * Denotes which bit to find the StatementType within the {@link #flags}.
	 * Type takes two bits.
	 */
    private static int TYPE_BIT = 0;

    /**
	 * Denotes which bit to find the ModifiedEnum within the {@link #flags}.
	 * Modified takes two bits.
	 */
    private static int MODIFIED_BIT = 2;

    /**
	 * Denotes which bit to find the userFlag within the {@link #flags}.
	 */
    private static int USERFLAG_BIT = 4;

    /**
	 * Denotes which bit to find the override flag within the {@link #flags}.
	 */
    private static int OVERRIDE_BIT = 5;

    /**
	 * Denotes which bit to find the sidable flag within the {@link #flags}.
	 */
    private static int SIDABLE_BIT = 6;

    
    final public IV get(final int index) {
        switch(index) {
        case 0: return s;
        case 1: return p;
        case 2: return o;
        case 3: return c();
        default: throw new IllegalArgumentException();
        }
    }
    
    final public IV s() {
        return s;
    }

    final public IV p() {
        return p;
    }

    final public IV o() {
        return o;
    }

    final public IV c() {
    	
    	// lazy instantiate the sid if necessary
    	if (c == null && sidable()) {
    		c = new SidIV(this);
    	}
    	
        return c;
        
    }
    
//    /**
//     * Set the statement identifier. This sets the 4th position of the quad, but
//     * some constraints are imposed on its argument.
//     * 
//     * @param sid
//     *            The statement identifier.
//     * 
//     * @throws IllegalArgumentException
//     *             if <i>sid</i> is {@link #NULL}.
//     * @throws IllegalStateException
//     *             if the statement identifier is already set.
//     */
//    public final void setStatementIdentifier(final IV sid) {
//
//        if (sid == null || !sid.isStatement()) {
//        	
//        	throw new IllegalArgumentException();
//        	
//        }
//        	
//        if (type() != StatementEnum.Explicit) {
//
//            // Only allowed for explicit statements.
//            throw new IllegalStateException();
//
//        }
//
//        sidable(true);
//
//        // set the current value for c
//        this.c = sid;
//
//    }

    /**
     * Set the statement identifier. This sets the 4th position of the quad, but
     * some constraints are imposed on its argument.
     * 
     * @param sid
     *            If sid is true, this ISPO will produce a sid on-demand when
     *            requested.
     */
    public final void setStatementIdentifier(final boolean sid) {

        if (sid && type() != StatementEnum.Explicit) {

            // Only allowed for explicit statements.
            throw new IllegalStateException();

        }

        sidable(sid);

        // clear the current value for c
        this.c = null;

    }

    public final IV getStatementIdentifier() {

    	if (!sidable())
             throw new IllegalStateException("No statement identifier: "
                    + toString());

    	// will lazy instantiate the sid
        return c();

    }

    final public boolean hasStatementIdentifier() {
        
        return sidable();
        
    }
    
    public void setOverride(final boolean override) {

        override(override);
        
    }

    public boolean isOverride() {
        
        return override();
        
    }

    /**
     * Triple constructor for a statement whose type is NOT known.
     * <p>
     * Note: This is primarily used when you want to discover the
     * type of the statement.
     * 
     * @see AbstractTripleStore#bulkCompleteStatements(ISPOIterator)
     */
    public SPO(final IV s, final IV p, final IV o) {
        
        this.s = s;
        this.p = p;
        this.o = o;
        type(null);
        
    }

    /**
     * Quads constructor.
     * 
     * @param s
     * @param p
     * @param o
     * @param c
     */
    public SPO(final IV s, final IV p, final IV o, final IV c) {

        this.s = s;
        this.p = p;
        this.o = o;
        this.c = c;
        type(null);

    }

    /**
     * Construct a triple.
     * <p>
     * Note: When the statement is {@link StatementEnum#Inferred} you MUST also
     * construct the appropriate {@link Justification}.
     * 
     * @param s
     * @param p
     * @param o
     * @param type
     *            The statement type.
     */
    public SPO(final IV s, final IV p, final IV o, StatementEnum type) {

        if (type == null)
            throw new IllegalArgumentException();
        
        this.s = s;
        this.p = p;
        this.o = o;
        type(type);
        
    }
    
    /**
     * Quads constructor with {@link StatementEnum}.
     * @param s
     * @param p
     * @param o
     * @param c
     * @param type
     */
    public SPO(final IV s, final IV p, final IV o, final IV c,
            final StatementEnum type) {

        if (type == null)
            throw new IllegalArgumentException();
        
        this.s = s;
        this.p = p;
        this.o = o;
        this.c = c;
        type(type);
        
    }

//    /**
//     * Constructor used when you know the {s,p,o} and have done a lookup in the
//     * index to determine whether or not the statement exists, its
//     * {@link StatementEnum} type, and its statement identifier (if assigned).
//     * 
//     * @param s
//     * @param p
//     * @param o
//     * @param val
//     */
//    public SPO(final long s, final long p, final long o, final byte[] val) {
//
//        this.s = s;
//        this.p = p;
//        this.o = o;
//        
//        decodeValue(this, val);
//        
//    }

    /**
     * Variant to create an {@link SPO} from constants (used by the unit tests).
     * 
     * @param s
     * @param p
     * @param o
     * @param type
     */
    public SPO(final IConstant<IV> s, final IConstant<IV> p,
            final IConstant<IV> o, final StatementEnum type) {

        this(s.get(), p.get(), o.get(), type);

    }

    /**
     * Variant to create an SPO from a predicate - the {@link StatementEnum} and
     * statement identifier are not specified. This may be used as a convenience
     * to extract the {s, p, o, c} from an {@link IPredicate} or from an
     * {@link IAccessPath} when the predicate is not known to be an
     * {@link SPOPredicate} or the {@link IAccessPath} is not known to be an
     * {@link SPOAccessPath}.
     * 
     * @param predicate
     *            The predicate.
     */
    @SuppressWarnings("unchecked")
    public SPO(final IPredicate<ISPO> predicate) {
        
        {

            final IVariableOrConstant<IV> t = predicate.get(0);

            s = t.isVar() ? null : t.get();

        }

        {

            final IVariableOrConstant<IV> t = predicate.get(1);

            p = t.isVar() ? null : t.get();

        }

        {

            final IVariableOrConstant<IV> t = predicate.get(2);

            o = t.isVar() ? null : t.get();

        }

        if (predicate.arity() >= 4) {

            final IVariableOrConstant<IV> t = predicate.get(3);

            c = t.isVar() ? null : t.get();

        } else {

            c = null;
            
        }

    }

    /**
     * Construct a triple from {@link BigdataValue}s and the specified statement
     * type.
     * 
     * @param s
     * @param p
     * @param o
     * @param type
     */
    public SPO(final BigdataResource s, final BigdataURI p,
            final BigdataValue o, final StatementEnum type) {

        this(s.getIV(), p.getIV(), o.getIV(), type);

    }

    /**
     * Construct a triple/quad from a {@link BigdataStatement}. The term
     * identifiers and statement type information available on the
     * {@link BigdataStatement} will be used to initialize the {@link SPO}.
     * 
     * @param stmt
     *            The statement.
     */
    public SPO(final BigdataStatement stmt) {
        
        this(stmt.s(),//
             stmt.p(),//
             stmt.o(),//
             stmt.c(),//
             stmt.getStatementType()//
             );
        
    }

    /**
     * Return <code>true</code> IFF the {@link SPO} is marked as {@link StatementEnum#Explicit}. 
     */
    public final boolean isExplicit() {
        
        return type() == StatementEnum.Explicit;
        
    }
    
    /**
     * Return <code>true</code> IFF the {@link SPO} is marked as {@link StatementEnum#Inferred}. 
     */
    public final boolean isInferred() {
        
        return type() == StatementEnum.Inferred;
        
    }
    
    /**
     * Return <code>true</code> IFF the {@link SPO} is marked as {@link StatementEnum#Axiom}. 
     */
    public final boolean isAxiom() {
        
        return type() == StatementEnum.Axiom;
        
    }
    
    /**
     * Return <code>true</code> IFF the {@link SPO} has the user flag bit set. 
     */
    public final boolean getUserFlag() {
        
        return userFlag();
        
    }
    
    /**
     * Set the user flag bit on this SPO.
     * 
     * @parm userFlag boolean flag
     */
    public final void setUserFlag(final boolean userFlag) {
        
        userFlag(userFlag);
        
    }
    
    private int hashCode = 0;

    /**
     * Hash code for the (s,p,o) per Sesame's {@link Statement#hashCode()}. It
     * DOES NOT consider the context position.
     */
    public int hashCode() {

        if (hashCode == 0) {

            final int s = this.s.hashCode();
            
            final int p = this.p.hashCode();
            
            final int o = this.o.hashCode();

            /*
             * Note: The historical behavior was based on the int64 term
             * identifiers. Since the hash code is now computed from the int32
             * hash codes of the (s,p,o) IV objects, the original bit math was
             * resulting in a hash code which was always zero (any 32 bit value
             * shifted right by 32 bits is zero).
             */
            hashCode = 961 * s + 31 * p + o;
//            hashCode = 961 * ((int) (s ^ (s >>> 32))) + 31
//                    * ((int) (p ^ (p >>> 32))) + ((int) (o ^ (o >>> 32)));

        }

        return hashCode;

    }

    public boolean equals(final Object o) {
        
        if (this == o)
            return true;

        if (o == null)
            return false;
        
        return equals((ISPO) o);
        
    }
    
    /**
     * True iff the {@link ISPO}s are the same object or if the same term
     * identifiers are assigned for the subject, predicate and object positions,
     * and the same {@link StatementEnum} are the same.
     * <p>
     * Note: This is NOT the same test as
     * {@link BigdataStatementImpl#equals(Object)} since the latter is
     * implemented per the {@link Statement} interface.
     */
    public boolean equals(final ISPO stmt2) {

        if (stmt2 == this)
            return true;

        return
                IVUtility.equals(this.s, stmt2.s()) && //
                IVUtility.equals(this.p, stmt2.p()) && //
                IVUtility.equals(this.o, stmt2.o()) && //
                type() == stmt2.getStatementType()
                ;

    }

    /**
     * Return a representation of the statement using the term identifiers (the
     * identifiers are NOT resolved to terms).
     * 
     * @see ITripleStore#toString(IV, IV, IV)
     */
    public String toString() {

        return ("< " + toString(s) + ", " + toString(p) + ", " + toString(o))
                + (c == null ? "" : ", " + toString(c))
                + (type() == null ? "" : " : " + type()
                        + (override() ? ", override" : ""))
                + (isModified() ? ", modified ("+modified()+")" : "") + " >";

    }
    
    /**
     * Represents the term identifier together with its type (literal, bnode,
     * uri, or statement identifier).
     * 
     * @param iv
     *            The term identifier.
     * @return
     */
    public static String toString(final IV iv) {

        if (iv == null)
            return IRawTripleStore.NULLSTR;

        return iv.toString();
        
    }

    /**
     * Resolves the term identifiers to terms against the store and returns a
     * representation of the statement using
     * {@link IRawTripleStore#toString(IV, IV, IV)}.
     * 
     * @param store
     *            The store (optional). When non-<code>null</code> the store
     *            will be used to resolve term identifiers to terms.
     * 
     * @return The externalized representation of the statement.
     */
    public String toString(final IRawTripleStore store) {
        
        if (store != null) {

            String t = null;
            
            if (type() != null) {
                switch(type()) {
                case Explicit    : t = "Explicit    "; break;
                case Inferred    : t = "Inferred    "; break;
                case Axiom       : t = "Axiom       "; break;
                default: throw new AssertionError();
                }
            } else {
                t = "Unknown     ";
            }

            return t + (isModified() ? "(*)" : "") + " : "
                    + store.toString(s, p, o, c);

        } else {
            
            return toString();
            
        }
        
    }

    final public boolean isFullyBound() {
    
        return s != null && p != null && o != null;

    }

    final public StatementEnum getStatementType() {

        return type();

    }

    final public void setStatementType(final StatementEnum type) {
        
        if(this.type() != null && this.type() != type) {
            
            throw new IllegalStateException("newValue="+type+", spo="+this);
            
        }
        
        type(type);
        
    }
    
    final public boolean hasStatementType() {
        
        return type() != null;
        
    }

    public boolean isModified() {
        
        return modified() != ModifiedEnum.NONE;
        
    }

    public void setModified(final ModifiedEnum modified) {

        modified(modified);

    }
    
    public ModifiedEnum getModified() {
        
        return modified();
        
    }

	/**
	 * Statement type is hiding in the 0 and 1 bits of the flags.
	 */
	private StatementEnum type() {
		
		// get just the 0 and 1 bits
//		final byte b = Bits.mask(flags, 0, 1);
		byte b = 0;
		b |= (0x1 << TYPE_BIT);
		b |= (0x1 << (TYPE_BIT+1));
		b &= flags;
		
        switch (b) {
        case 0: return null;
        case 1: return StatementEnum.Explicit;
        case 2: return StatementEnum.Axiom;
        case 3: return StatementEnum.Inferred;
        }
        
        throw new IllegalStateException();
        
	}
	
	/**
	 * Statement type is hiding in the 0 and 1 bits of the flags.
	 */
	private void type(final StatementEnum type) {

		byte b = flags;
		
		if (type == null) {
			b = Bits.set(Bits.set(b, TYPE_BIT, false), (TYPE_BIT+1), false);
		} else {
			switch(type) {
			case Explicit: 
				b = Bits.set(Bits.set(b, TYPE_BIT, true), (TYPE_BIT+1), false);
				break;
			case Axiom: 
				b = Bits.set(Bits.set(b, TYPE_BIT, false), (TYPE_BIT+1), true);
				break;
			case Inferred:
				b = Bits.set(Bits.set(b, TYPE_BIT, true), (TYPE_BIT+1), true);
				break;
			default:
		        throw new IllegalStateException();
			}
		}
		
		flags = b;
		
	}
	
	/**
	 * Modified enum is hiding in the 2 and 3 bits of the flags.
	 */
	private ModifiedEnum modified() {
		
		// get just the 2 and 3 bits
//		final byte b = Bits.mask(flags, 2, 3);
		byte b = 0;
		b |= (0x1 << MODIFIED_BIT);
		b |= (0x1 << (MODIFIED_BIT+1));
		b &= flags;
		
        switch (b) {
        case 0: return ModifiedEnum.NONE;
        case 4: return ModifiedEnum.INSERTED;
        case 8: return ModifiedEnum.REMOVED;
        case 12: return ModifiedEnum.UPDATED;
        }
        
        throw new IllegalStateException();

	}
	
	/**
	 * Modified enum is hiding in the 2 and 3 bits of the flags.
	 */
	private void modified(final ModifiedEnum modified) {
		
		byte b = flags;
		
		if (modified == null) {
			throw new IllegalArgumentException();
		} else {
			switch(modified) {
			case NONE: 
				b = Bits.set(Bits.set(b, MODIFIED_BIT, false), (MODIFIED_BIT+1), false);
				break;
			case INSERTED: 
				b = Bits.set(Bits.set(b, MODIFIED_BIT, true), (MODIFIED_BIT+1), false);
				break;
			case REMOVED: 
				b = Bits.set(Bits.set(b, MODIFIED_BIT, false), (MODIFIED_BIT+1), true);
				break;
			case UPDATED:
				b = Bits.set(Bits.set(b, MODIFIED_BIT, true), (MODIFIED_BIT+1), true);
				break;
			default:
		        throw new IllegalStateException();
			}
		}
		
		flags = b;
		
	}
	
	/**
	 * User flag is hiding in the 4 bit of the flags.
	 */
	private boolean userFlag() {
		return Bits.get(flags, USERFLAG_BIT);
	}
	
	/**
	 * User flag is hiding in the 4 bit of the flags.
	 */
	private void userFlag(final boolean userFlag) {
		flags = Bits.set(flags, USERFLAG_BIT, userFlag);
	}
	
	/**
	 * Override is hiding in the 5 bit of the flags.
	 */
	private boolean override() {
		return Bits.get(flags, OVERRIDE_BIT);
	}
	
	/**
	 * Override is hiding in the 5 bit of the flags.
	 */
	private void override(final boolean override) {
		flags = Bits.set(flags, OVERRIDE_BIT, override);
	}
	
	/**
	 * Sidable is hiding in the 6 bit of the flags.
	 */
	private boolean sidable() {
		return Bits.get(flags, SIDABLE_BIT);
	}
	
	/**
	 * Sidable is hiding in the 6 bit of the flags.
	 */
	private void sidable(final boolean sidable) {
		flags = Bits.set(flags, SIDABLE_BIT, sidable);
	}
	
}
