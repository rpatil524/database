/**

Copyright (C) SYSTAP, LLC 2006-2010.  All rights reserved.

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
package com.bigdata.ha.msg;

import java.util.UUID;

/**
 * Message requesting a root block for a store on a remote service.
 */
public interface IHARootBlockRequest extends IHAMessage {

    /**
     * The {@link UUID} of the journal whose root block will be returned
     * (optional, defaults to the current Journal).
     * <p>
     * Note: This parameter is intended for scale-out if there is a need to
     * fetch the root block of a historical journal (versus the live journal).
     */
    UUID getStoreUUID();

}