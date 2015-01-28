/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * This file Copyright (C) 2015 Jeremy D Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.message;

import java.util.List;

/**
 * Message from server for i18n localization of items such as game options or scenarios.
 *<P>
 * The first element of {@link #getParams()} is a string type such as {@link #TYPE_GAMEOPT}
 * or {@link #TYPE_SCENARIO}.  This is followed by the integer flag field (hex string) which
 * is removed from the list at the receiving end's parser.
 * The rest of {@code getParams()} is organized according to the type; see type constant javadocs.
 * Since {@code getParams()} can't contain empty strings, check contents for {@link #EMPTY}.
 *<P>
 * Not a per-game message; {@link #getGame()} returns {@link SOCMessage#GAME_NONE}.
 *<P>
 * Robot clients don't need to know about or handle this message type,
 * because they don't have a locale.
 *<P>
 * Introduced in 2.0.00; check client version against {@link soc.util.SOCStringManager#VERSION_FOR_I18N}.
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 2.0.00
 */
public class SOCLocalizedStrings extends SOCMessageTemplateMs
{
    /**
     * Symbol to represent a null or empty string value, because
     * empty {@code pa[]} elements can't be parsed over the network.
     */
    public static final String EMPTY = "\t";

    /**
     * Game Option localized names, for {@link soc.game.SOCGameOption}.
     * After the string type at element 0, {@link #getParams()} contents are pairs
     * of strings, each of which is a game option keyname and localized name.
     */
    public static final String TYPE_GAMEOPT = "G";

    /**
     * Game Scenario localized names and descriptions, for {@link soc.game.SOCScenario}. After the
     * string type at element 0, {@link #getParams()} contents are triples of strings, each of which
     * is a game scenario keyname, localized name, and optional localized long description or {@link #EMPTY}.
     *<P>
     * If the client has requested specific scenario keynames for this type, the server replies with all known
     * localized strings for those items.  Items without localized strings will not be included in the reply.
     * If none of the items are localized, server replies with an empty list with the {@link #FLAG_SENT_ALL}
     * flag.
     */
    public static final String TYPE_SCENARIO = "S";

    /** First character of all MARKERs */
    private static final String MARKER_PREFIX = "\026";  // 0x16 ^V (SYN)

    /**
     * "Type is unknown" flag, for server's response when it doesn't recognize
     * the string type requested by the client.
     */
    public static final int FLAG_TYPE_UNKNOWN = 0x01;

    /**
     * "Sent all of them" flag, for server's response when it has sent all known items
     * of the string type requested by the client.
     *<P>
     * This flag can also be sent when no known items are available for a recognized
     * string type; the server will send an empty list with this flag set.
     */
    public static final int FLAG_SENT_ALL = 0x02;

    /**
     * "Key is unknown" flag, for server's response when it doesn't have a localized
     * string for the key requested by the client.
     */
    public static final String MARKER_KEY_UNKNOWN = "\026K";

    private static final long serialVersionUID = 2000L;  // no structural change since introduced in v2.0.00

    /**
     * Request or response flags such as {@link #FLAG_SENT_ALL}.
     * @see #isFlagSet(int)
     */
    private int flags;

    /**
     * Constructor for client to parse server's list of localized strings.
     * See {@link SOCLocalizedStrings class javadoc} for interpreting contents of this message.
     *<P>
     * The {@link #flags} field (for {@link #FLAG_SENT_ALL}, {@link #FLAG_TYPE_UNKNOWN}, etc)
     * is parsed here and removed from the list of strings.
     *<P>
     * There is no server-side constructor, the server instead calls {@link #toCmd(String, int, List)}.
     *
     * @param str String list; assumes caller has validated length >= 2 (type, flags).
     * @throws NumberFormatException  if flags field isn't a valid hex number
     */
    private SOCLocalizedStrings(final List<String> strs)
        throws NumberFormatException
    {
        super(LOCALIZEDSTRINGS, SOCMessage.GAME_NONE, strs);

        // flag field; parse error throws exception for parseDataStr to catch
        flags = Integer.parseInt(strs.get(1), 16);
        strs.remove(1);
    }

    /**
     * Is this flag bit set in the {@code flags} field?
     * @param flag  A flag such as {@link #FLAG_SENT_ALL}
     * @return  True if set
     */
    public final boolean isFlagSet(final int flag)
    {
        return (0 != (flags & flag));
    }

    /**
     * Minimum version where this message type is used.
     * LOCALIZEDSTRINGS introduced in 2.0.00 for i18n localization.
     * @return Version number, 2000 for JSettlers 2.0.00.
     */
    @Override
    public final int getMinimumVersion() { return 2000; }

    /**
     * Parse the command String list into a SOCLocalizedStrings message.
     *
     * @param strs  the data list; length must be at least 1 to indicate the type
     * @return    a SOCLocalizedStrings message, or null if parsing errors
     */
    public static SOCLocalizedStrings parseDataStr(List<String> strs)
    {
        if ((strs == null) || (strs.size() < 2))
            return null;  // must have at least 2 strings: type, flags

        try
        {
            return new SOCLocalizedStrings(strs);
        } catch (Exception e) {
            // catch NumberFormatException and anything else from a malformed message
            return null;
        }
    }

    /**
     * Build the command string from a type and list of strings; used at server side.
     * @param type  String type such as {@link #TYPE_SCENARIO};
     *     must pass {@link SOCMessage#isSingleLineAndSafe(String)}.
     * @param flags  Any flags such as {@link #FLAG_SENT_ALL}, or 0
     * @param strs  the list of strings, organized in a type-specific way; see {@code type} constant javadocs.
     *     Each element must pass
     *     {@link SOCMessage#isSingleLineAndSafe(String, boolean) isSingleLineAndSafe(String, true)}:
     *     {@link SOCMessage#sep2} characters are allowed, but {@link SOCMessage#sep} are not.
     *    <P>
     *     The list may be empty but not null.  Empty or null elements are automatically replaced here
     *     with {@link #EMPTY}, but {@link #getParams()} will not automatically replace {@link #EMPTY}
     *     with "" at the receiver.
     * @return    the command string
     * @throws IllegalArgumentException  If {@code type} or any element of {@code strs} fails
     *     {@link SOCMessage#isSingleLineAndSafe(String)}.
     * @throws NullPointerException if {@code strs} is null
     */
    public static String toCmd(final String type, final int flags, List<String> strs)
        throws IllegalArgumentException, NullPointerException
    {
        if (! isSingleLineAndSafe(type))
            throw new IllegalArgumentException("type: " + type);

        StringBuilder sb = new StringBuilder(Integer.toString(SOCMessage.LOCALIZEDSTRINGS));
        sb.append(sep);
        sb.append(type);
        sb.append(sep);
        sb.append(Integer.toHexString(flags));

        for (int i = 0; i < strs.size(); ++i)
        {
            sb.append(sep);

            String itm = strs.get(i);
            if ((itm == null) || (itm.length() == 0))
                itm = EMPTY;
            else if (itm.startsWith(MARKER_PREFIX))
            {
                if (! itm.equals(MARKER_KEY_UNKNOWN))
                    throw new IllegalArgumentException("item " + i + ": " + itm);
            }
            else if ((itm.indexOf(SOCMessage.sep_char) != -1) || ! isSingleLineAndSafe(itm, true))
                throw new IllegalArgumentException("item " + i + ": " + itm);

            sb.append(itm);
        }

        return sb.toString();
    }

}
