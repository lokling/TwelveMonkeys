/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util.regex;

import java.io.PrintStream;

/**
 * This class parses arbitrary strings against a wildcard string mask provided.
 * The wildcard characters are '*' and '?'.
 * <p/>
 * The string masks provided are treated as case sensitive.<br>
 * Null-valued string masks as well as null valued strings to be parsed, will lead to rejection.
 * <p/>
 * <p/>
 * <p/>
 * <i>This class is custom designed for wildcard string parsing and is several times faster than the implementation based on the Jakarta Regexp package.</i>
 * <p/>
 * <p><hr style="height=1"><p>
 * <p/>
 * This task is performed based on regular expression techniques.
 * The possibilities of string generation with the well-known wildcard characters stated above,
 * represent a subset of the possibilities of string generation with regular expressions.<br>
 * The '*' corresponds to ([Union of all characters in the alphabet])*<br>
 * The '?' corresponds to ([Union of all characters in the alphabet])<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<small>These expressions are not suited for textual representation at all, I must say. Is there any math tags included in HTML?</small>
 * <p/>
 * <p/>
 * <p/>
 * The complete meta-language for regular expressions are much larger.
 * This fact makes it fairly straightforward to build data structures for parsing because the amount of rules of building these structures are quite limited, as stated below.
 * <p/>
 * <p/>
 * <p/>
 * To bring this over to mathematical terms:
 * The parser ia a <b>nondeterministic finite automaton</b> (latin) representing the <b>grammar</b> which is stated by the string mask.
 * The <b>language</b> accepted by this automaton is the set of all strings accepted by this automaton.<br>
 * The formal automaton quintuple consists of:
 * <ol>
 * <li>A finite set of <b>states</b>, depending on the wildcard string mask.
 * For each character in the mask a state representing that character is created.
 * The number of states therefore coincides with the length of the mask.
 * <li>An <b>alphabet</b> consisting of all legal filename characters - included the two wildcard characters '*' and '?'.
 * This alphabet is hard-coded in this class. It contains {a .. �}, {A .. �}, {0 .. 9}, {.}, {_}, {-}, {*} and {?}.
 * <li>A finite set of <b>initial states</b>, here only consisting of the state corresponding to the first character in the mask.
 * <li>A finite set of <b>final states</b>, here only consisting of the state corresponding to the last character in the mask.
 * <li>A <b>transition relation</b> that is a finite set of transitions satisfying some formal rules.<br>
 * This implementation on the other hand, only uses ad-hoc rules which start with an initial setup of the states as a sequence according to the string mask.<br>
 * Additionally, the following rules completes the building of the automaton:
 * <ol>
 * <li>If the next state represents the same character as the next character in the string to test - go to this next state.
 * <li>If the next state represents '*' - go to this next state.
 * <li>If the next state represents '?' - go to this next state.
 * <li>If a '*' is followed by one or more '?', the last of these '?' state counts as a '*' state. Some extra checks regarding the number of characters read must be imposed if this is the case...
 * <li>If the next character in the string to test does not coincide with the next state - go to the last state representing '*'. If there are none - rejection.
 * <li>If there are no subsequent state (final state) and the state represents '*' - acceptance.
 * <li>If there are no subsequent state (final state) and the end of the string to test is reached - acceptance.
 * </ol>
 * <br>
 * <small>
 * Disclaimer: This class does not build a finite automaton according to formal mathematical rules.
 * The proper way of implementation should be finding the complete set of transition relations, decomposing these into rules accepted by a <i>deterministic</i> finite automaton and finally build this automaton to be used for string parsing.
 * Instead, this class is ad-hoc implemented based on the informal transition rules stated above.
 * Therefore the correctness cannot be guaranteed before extensive testing has been imposed on this class... anyway, I think I have succeeded.
 * Parsing faults must be reported to the author.
 * </small>
 * </ol>
 * <p/>
 * <p><hr style="height=1"><p>
 * <p/>
 * Examples of usage:<br>
 * This example will return "Accepted!".
 * <pre>
 * WildcardStringParser parser = new WildcardStringParser("*_28????.jp*");
 * if (parser.parseString("gupu_280915.jpg")) {
 *     System.out.println("Accepted!");
 * } else {
 *     System.out.println("Not accepted!");
 * }
 * </pre>
 * <p/>
 * <p><hr style="height=1"><p>
 * <p/>
 * Theories and concepts are based on the book <i>Elements of the Theory of Computation</i>, by Harry l. Lewis and Christos H. Papadimitriou, (c) 1981 by Prentice Hall.
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:eirik.torske@iconmedialab.no">Eirik Torske</a>
 * @deprecated Will probably be removed in the near future
 */
public class WildcardStringParser {

    // Constants

    /** Field ALPHABET */
    public static final char[] ALPHABET = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '�',
            '�', '�', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'N', 'M', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
            'Z', '�', '�', '�', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '_', '-'
    };

    /** Field FREE_RANGE_CHARACTER */
    public static final char FREE_RANGE_CHARACTER = '*';

    /** Field FREE_PASS_CHARACTER */
    public static final char FREE_PASS_CHARACTER = '?';

    // Members
    boolean mInitialized;
    String mStringMask;
    WildcardStringParserState mInitialState;
    int mTotalNumberOfStringsParsed;
    boolean mDebugging;
    PrintStream out;

    // Properties
    // Constructors

    /**
     * Creates a wildcard string parser.
     * <p/>
     *
     * @param pStringMask the wildcard string mask.
     */
    public WildcardStringParser(final String pStringMask) {
        this(pStringMask, false);
    }

    /**
     * Creates a wildcard string parser.
     * <p/>
     *
     * @param pStringMask the wildcard string mask.
     * @param pDebugging {@code true} will cause debug messages to be emitted to {@code System.out}.
     */
    public WildcardStringParser(final String pStringMask, final boolean pDebugging) {
        this(pStringMask, pDebugging, System.out);
    }

    /**
     * Creates a wildcard string parser.
     * <p/>
     *
     * @param pStringMask the wildcard string mask.
     * @param pDebugging {@code true} will cause debug messages to be emitted.
     * @param pDebuggingPrintStream the {@code java.io.PrintStream} to which the debug messages will be emitted.
     */
    public WildcardStringParser(final String pStringMask, final boolean pDebugging, final PrintStream pDebuggingPrintStream) {

        this.mStringMask = pStringMask;
        this.mDebugging = pDebugging;
        this.out = pDebuggingPrintStream;
        mInitialized = buildAutomaton();
    }

    // Methods
    private boolean checkIfStateInWildcardRange(WildcardStringParserState pState) {

        WildcardStringParserState runnerState = pState;

        while (runnerState.mPreviousState != null) {
            runnerState = runnerState.mPreviousState;
            if (isFreeRangeCharacter(runnerState.mChar)) {
                return true;
            }
            if (!isFreePassCharacter(runnerState.mChar)) {
                return false;
            }  // If free-pass char '?' - move on
        }
        return false;
    }

    private boolean checkIfLastFreeRangeState(WildcardStringParserState pState) {

        if (isFreeRangeCharacter(pState.mChar)) {
            return true;
        }
        if (isFreePassCharacter(pState.mChar)) {
            if (checkIfStateInWildcardRange(pState)) {
                return true;
            }
        }
        return false;
    }

    /** @return {@code true} if and only if the string mask only consists of free-range wildcard character(s). */
    private boolean isTrivialAutomaton() {

        for (int i = 0; i < mStringMask.length(); i++) {
            if (!isFreeRangeCharacter(mStringMask.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean buildAutomaton() {

        char activeChar;
        WildcardStringParserState runnerState = null;
        WildcardStringParserState newState = null;
        WildcardStringParserState lastFreeRangeState = null;

        // Create the initial state of the automaton
        if ((mStringMask != null) && (mStringMask.length() > 0)) {
            newState = new WildcardStringParserState(mStringMask.charAt(0));
            newState.mAutomatonStateNumber = 0;
            newState.mPreviousState = null;
            if (checkIfLastFreeRangeState(newState)) {
                lastFreeRangeState = newState;
            }
            runnerState = newState;
            mInitialState = runnerState;
            mInitialState.mAutomatonStateNumber = 0;
        }
        else {
            System.err.println("string mask provided are null or empty - aborting!");
            return false;
        }

        // Create the rest of the automaton
        for (int i = 1; i < mStringMask.length(); i++) {
            activeChar = mStringMask.charAt(i);

            // Check if the char is an element in the alphabet or is a wildcard character
            if (!((isInAlphabet(activeChar)) || (isWildcardCharacter(activeChar)))) {
                System.err.println("one or more characters in string mask are not legal characters - aborting!");
                return false;
            }

            // Set last free-range state before creating/checking the next state
            runnerState.mLastFreeRangeState = lastFreeRangeState;

            // Create next state, check if free-range state, set the state number and preceeding state
            newState = new WildcardStringParserState(activeChar);
            newState.mAutomatonStateNumber = i;
            newState.mPreviousState = runnerState;

            // Special check if the state represents an '*' or '?' with only preceeding states representing '?' and '*'
            if (checkIfLastFreeRangeState(newState)) {
                lastFreeRangeState = newState;
            }

            // Set the succeding state before moving to the next state
            runnerState.mNextState = newState;

            // Move to the next state
            runnerState = newState;

            // Special setting of the last free-range state for the last element
            if (runnerState.mAutomatonStateNumber == mStringMask.length() - 1) {
                runnerState.mLastFreeRangeState = lastFreeRangeState;
            }
        }

        // Initiate some statistics
        mTotalNumberOfStringsParsed = 0;
        return true;
    }

    /** Tests if a certain character is a valid character in the alphabet that is applying for this automaton. */
    public static boolean isInAlphabet(final char pCharToCheck) {

        for (int i = 0; i < ALPHABET.length; i++) {
            if (pCharToCheck == ALPHABET[i]) {
                return true;
            }
        }
        return false;
    }

    /** Tests if a certain character is the designated "free-range" character ('*'). */
    public static boolean isFreeRangeCharacter(final char pCharToCheck) {
        return pCharToCheck == FREE_RANGE_CHARACTER;
    }

    /** Tests if a certain character is the designated "free-pass" character ('?'). */
    public static boolean isFreePassCharacter(final char pCharToCheck) {
        return pCharToCheck == FREE_PASS_CHARACTER;
    }

    /** Tests if a certain character is a wildcard character ('*' or '?'). */
    public static boolean isWildcardCharacter(final char pCharToCheck) {
        return ((isFreeRangeCharacter(pCharToCheck)) || (isFreePassCharacter(pCharToCheck)));
    }

    /**
     * Gets the string mask that was used when building the parser atomaton.
     * <p/>
     *
     * @return the string mask used for building the parser automaton.
     */
    public String getStringMask() {
        return mStringMask;
    }

    /**
     * Parses a string according to the rules stated above.
     * <p/>
     *
     * @param pStringToParse the string to parse.
     * @return {@code true} if and only if the string are accepted by the automaton.
     */
    public boolean parseString(final String pStringToParse) {

        if (mDebugging) {
            out.println("parsing \"" + pStringToParse + "\"...");
        }

        // Update statistics
        mTotalNumberOfStringsParsed++;

        // Check string to be parsed for nullness
        if (pStringToParse == null) {
            if (mDebugging) {
                out.println("string to be parsed is null - rejection!");
            }
            return false;
        }

        // Create parsable string
        ParsableString parsableString = new ParsableString(pStringToParse);

        // Check string to be parsed
        if (!parsableString.checkString()) {
            if (mDebugging) {
                out.println("one or more characters in string to be parsed are not legal characters - rejection!");
            }
            return false;
        }

        // Check if automaton is correctly initialized
        if (!mInitialized) {
            System.err.println("automaton is not initialized - rejection!");
            return false;
        }

        // Check if automaton is trivial (accepts all strings)
        if (isTrivialAutomaton()) {
            if (mDebugging) {
                out.println("automaton represents a trivial string mask (accepts all strings) - acceptance!");
            }
            return true;
        }

        // Check if string to be parsed is empty
        if (parsableString.isEmpty()) {
            if (mDebugging) {
                out.println("string to be parsed is empty and not trivial automaton - rejection!");
            }
            return false;
        }

        // Flag and more to indicate that state skipping due to sequence of '?' succeeding a '*' has been performed
        boolean hasPerformedFreeRangeMovement = false;
        int numberOfFreePassCharactersRead_SinceLastFreePassState = 0;
        int numberOfParsedCharactersRead_SinceLastFreePassState = 0;
        WildcardStringParserState runnerState = null;

        // Accepted by the first state?
        if ((parsableString.mCharArray[0] == mInitialState.mChar) || isWildcardCharacter(mInitialState.mChar)) {
            runnerState = mInitialState;
            parsableString.mIndex = 0;
        }
        else {
            if (mDebugging) {
                out.println("cannot enter first automaton state - rejection!");
            }
            return false;
        }

        // Initialize the free-pass character state visited count
        if (isFreePassCharacter(runnerState.mChar)) {
            numberOfFreePassCharactersRead_SinceLastFreePassState++;
        }

        // Perform parsing according to the rules above
        for (int i = 0; i < parsableString.length(); i++) {
            if (mDebugging) {
                out.println();
            }
            if (mDebugging) {
                out.println("parsing - index number " + i + ", active char: '"
                        + parsableString.getActiveChar() + "' char string index: " + parsableString.mIndex
                        + " number of chars since last free-range state: " + numberOfParsedCharactersRead_SinceLastFreePassState);
            }
            if (mDebugging) {
                out.println("parsing - state: " + runnerState.mAutomatonStateNumber + " '"
                        + runnerState.mChar + "' - no of free-pass chars read: " + numberOfFreePassCharactersRead_SinceLastFreePassState);
            }
            if (mDebugging) {
                out.println("parsing - hasPerformedFreeRangeMovement: " + hasPerformedFreeRangeMovement);
            }
            if (runnerState.mNextState == null) {
                if (mDebugging) {
                    out.println("parsing - runnerState.mNextState == null");
                }

                // If there are no subsequent state (final state) and the state represents '*' - acceptance!
                if (isFreeRangeCharacter(runnerState.mChar)) {

                    // Special free-range skipping check
                    if (hasPerformedFreeRangeMovement) {
                        if (parsableString.reachedEndOfString()) {
                            if (numberOfFreePassCharactersRead_SinceLastFreePassState > numberOfParsedCharactersRead_SinceLastFreePassState) {
                                if (mDebugging) {
                                    out.println(
                                            "no subsequent state (final state) and the state represents '*' - end of parsing string, but not enough characters read - rejection!");
                                }
                                return false;
                            }
                            else {
                                if (mDebugging) {
                                    out.println(
                                            "no subsequent state (final state) and the state represents '*' - end of parsing string and enough characters read - acceptance!");
                                }
                                return true;
                            }
                        }
                        else {
                            if (numberOfFreePassCharactersRead_SinceLastFreePassState > numberOfParsedCharactersRead_SinceLastFreePassState) {
                                if (mDebugging) {
                                    out.println(
                                            "no subsequent state (final state) and the state represents '*' - not the end of parsing string and not enough characters read - read next character");
                                }
                                parsableString.mIndex++;
                                numberOfParsedCharactersRead_SinceLastFreePassState++;
                            }
                            else {
                                if (mDebugging) {
                                    out.println(
                                            "no subsequent state (final state) and the state represents '*' - not the end of parsing string, but enough characters read - acceptance!");
                                }
                                return true;
                            }
                        }
                    }
                    else {
                        if (mDebugging) {
                            out.println("no subsequent state (final state) and the state represents '*' - no skipping performed - acceptance!");
                        }
                        return true;
                    }
                }

                // If there are no subsequent state (final state) and no skipping has been performed and the end of the string to test is reached - acceptance!
                else if (parsableString.reachedEndOfString()) {

                    // Special free-range skipping check
                    if ((hasPerformedFreeRangeMovement)
                            && (numberOfFreePassCharactersRead_SinceLastFreePassState > numberOfParsedCharactersRead_SinceLastFreePassState)) {
                        if (mDebugging) {
                            out.println(
                                    "no subsequent state (final state) and skipping has been performed and end of parsing string, but not enough characters read - rejection!");
                        }
                        return false;
                    }
                    if (mDebugging) {
                        out.println("no subsequent state (final state) and the end of the string to test is reached - acceptance!");
                    }
                    return true;
                }
                else {
                    if (mDebugging) {
                        out.println("parsing - escaping process...");
                    }
                }
            }
            else {
                if (mDebugging) {
                    out.println("parsing - runnerState.mNextState != null");
                }

                // Special Case:
                // If this state represents '*' - go to the rightmost state representing '?'.
                //    This state will act as an '*' - except that you only can go to the next state or accept the string, if and only if the number of '?' read are equal or less than the number of character read from the parsing string.
                if (isFreeRangeCharacter(runnerState.mChar)) {
                    numberOfFreePassCharactersRead_SinceLastFreePassState = 0;
                    numberOfParsedCharactersRead_SinceLastFreePassState = 0;
                    WildcardStringParserState freeRangeRunnerState = runnerState.mNextState;

                    while ((freeRangeRunnerState != null) && (isFreePassCharacter(freeRangeRunnerState.mChar))) {
                        runnerState = freeRangeRunnerState;
                        hasPerformedFreeRangeMovement = true;
                        numberOfFreePassCharactersRead_SinceLastFreePassState++;
                        freeRangeRunnerState = freeRangeRunnerState.mNextState;
                    }

                    // Special Case: if the mask is at the end
                    if (runnerState.mNextState == null) {
                        if (mDebugging) {
                            out.println();
                        }
                        if (mDebugging) {
                            out.println("parsing - index number " + i + ", active char: '"
                                    + parsableString.getActiveChar() + "' char string index: " + parsableString.mIndex
                                    + " number of chars since last free-range state: " + numberOfParsedCharactersRead_SinceLastFreePassState);
                        }
                        if (mDebugging) {
                            out.println("parsing - state: " + runnerState.mAutomatonStateNumber + " '"
                                    + runnerState.mChar + "' - no of free-pass chars read: " + numberOfFreePassCharactersRead_SinceLastFreePassState);
                        }
                        if (mDebugging) {
                            out.println("parsing - hasPerformedFreeRangeMovement: "
                                    + hasPerformedFreeRangeMovement);
                        }
                        if ((hasPerformedFreeRangeMovement)
                                && (numberOfFreePassCharactersRead_SinceLastFreePassState >= numberOfParsedCharactersRead_SinceLastFreePassState)) {
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                }

                // If the next state represents '*' - go to this next state
                if (isFreeRangeCharacter(runnerState.mNextState.mChar)) {
                    runnerState = runnerState.mNextState;
                    parsableString.mIndex++;
                    numberOfParsedCharactersRead_SinceLastFreePassState++;
                }

                // If the next state represents '?' - go to this next state
                else if (isFreePassCharacter(runnerState.mNextState.mChar)) {
                    runnerState = runnerState.mNextState;
                    parsableString.mIndex++;
                    numberOfFreePassCharactersRead_SinceLastFreePassState++;
                    numberOfParsedCharactersRead_SinceLastFreePassState++;
                }

                // If the next state represents the same character as the next character in the string to test - go to this next state
                else if ((!parsableString.reachedEndOfString()) && (runnerState.mNextState.mChar == parsableString.getSubsequentChar())) {
                    runnerState = runnerState.mNextState;
                    parsableString.mIndex++;
                    numberOfParsedCharactersRead_SinceLastFreePassState++;
                }

                // If the next character in the string to test does not coincide with the next state - go to the last state representing '*'. If there are none - rejection!
                else if (runnerState.mLastFreeRangeState != null) {
                    runnerState = runnerState.mLastFreeRangeState;
                    parsableString.mIndex++;
                    numberOfParsedCharactersRead_SinceLastFreePassState++;
                }
                else {
                    if (mDebugging) {
                        out.println("the next state does not represent the same character as the next character in the string to test, and there are no last-free-range-state - rejection!");
                    }
                    return false;
                }
            }
        }
        if (mDebugging) {
            out.println("finished reading parsing string and not at any final state - rejection!");
        }
        return false;
    }

    /*
    * Overriding mandatory methods from EntityObject's.
    */

    /**
     * Method toString
     *
     * @return
     */
    public String toString() {

        StringBuilder buffer = new StringBuilder();

        if (!mInitialized) {
            buffer.append(getClass().getName());
            buffer.append(":  Not initialized properly!");
            buffer.append("\n");
            buffer.append("\n");
        }
        else {
            WildcardStringParserState runnerState = mInitialState;

            buffer.append(getClass().getName());
            buffer.append(":  String mask ");
            buffer.append(mStringMask);
            buffer.append("\n");
            buffer.append("\n");
            buffer.append("      Automaton: ");
            while (runnerState != null) {
                buffer.append(runnerState.mAutomatonStateNumber);
                buffer.append(": ");
                buffer.append(runnerState.mChar);
                buffer.append(" (");
                if (runnerState.mLastFreeRangeState != null) {
                    buffer.append(runnerState.mLastFreeRangeState.mAutomatonStateNumber);
                }
                else {
                    buffer.append("-");
                }
                buffer.append(")");
                if (runnerState.mNextState != null) {
                    buffer.append("   -->   ");
                }
                runnerState = runnerState.mNextState;
            }
            buffer.append("\n");
            buffer.append("      Format: <state index>: <character> (<last free state>)");
            buffer.append("\n");
            buffer.append("      Number of strings parsed: " + mTotalNumberOfStringsParsed);
            buffer.append("\n");
        }
        return buffer.toString();
    }

    /**
     * Method equals
     *
     * @param pObject
     * @return
     */
    public boolean equals(Object pObject) {

        if (pObject instanceof WildcardStringParser) {
            WildcardStringParser externalParser = (WildcardStringParser) pObject;

            return ((externalParser.mInitialized == this.mInitialized) && (externalParser.mStringMask == this.mStringMask));
        }
        return super.equals(pObject);
    }

    // Just taking the lazy, easy and dangerous way out

    /**
     * Method hashCode
     *
     * @return
     */
    public int hashCode() {
        return super.hashCode();
    }

    protected Object clone() throws CloneNotSupportedException {

        if (mInitialized) {
            return new WildcardStringParser(mStringMask);
        }
        return null;
    }

    // Just taking the lazy, easy and dangerous way out
    protected void finalize() throws Throwable {
    }

    /** A simple holder class for an automaton state. */
    class WildcardStringParserState {

        // Constants
        // Members
        int mAutomatonStateNumber;
        char mChar;
        WildcardStringParserState mPreviousState;
        WildcardStringParserState mNextState;
        WildcardStringParserState mLastFreeRangeState;

        // Constructors

        /**
         * Constructor WildcardStringParserState
         *
         * @param pChar
         */
        public WildcardStringParserState(final char pChar) {
            this.mChar = pChar;
        }

        // Methods
        // Debug
    }

    /** A simple holder class for a string to be parsed. */
    class ParsableString {

        // Constants
        // Members
        char[] mCharArray;
        int mIndex;

        // Constructors
        ParsableString(final String pStringToParse) {

            if (pStringToParse != null) {
                mCharArray = pStringToParse.toCharArray();
            }
            mIndex = -1;
        }

        // Methods
        boolean reachedEndOfString() {

            //System.out.println(DebugUtil.DEBUG + DebugUtil.getClassName(this) + ": mIndex            :" + mIndex);
            //System.out.println(DebugUtil.DEBUG + DebugUtil.getClassName(this) + ": mCharArray.length :" + mCharArray.length);
            return mIndex == mCharArray.length - 1;
        }

        int length() {
            return mCharArray.length;
        }

        char getActiveChar() {

            if ((mIndex > -1) && (mIndex < mCharArray.length)) {
                return mCharArray[mIndex];
            }
            System.err.println(getClass().getName() + ": trying to access character outside character array!");
            return ' ';
        }

        char getSubsequentChar() {

            if ((mIndex > -1) && (mIndex + 1 < mCharArray.length)) {
                return mCharArray[mIndex + 1];
            }
            System.err.println(getClass().getName() + ": trying to access character outside character array!");
            return ' ';
        }

        boolean checkString() {

            if (!isEmpty()) {

                // Check if the string only contains chars that are elements in the alphabet
                for (int i = 0; i < mCharArray.length; i++) {
                    if (!WildcardStringParser.isInAlphabet(mCharArray[i])) {
                        return false;
                    }
                }
            }
            return true;
        }

        boolean isEmpty() {
            return ((mCharArray == null) || (mCharArray.length == 0));
        }

        /**
         * Method toString
         *
         * @return
         */
        public String toString() {
            return new String(mCharArray);
        }
    }
}


/*--- Formatted in Sun Java Convention Style on ma, des 1, '03 ---*/


/*------ Formatted by Jindent 3.23 Basic 1.0 --- http://www.jindent.de ------*/
