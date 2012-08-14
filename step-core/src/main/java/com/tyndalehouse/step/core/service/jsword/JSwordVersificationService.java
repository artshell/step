package com.tyndalehouse.step.core.service.jsword;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.passage.Verse;
import org.crosswire.jsword.passage.VerseRange;
import org.crosswire.jsword.versification.Versification;

/**
 * Some generally useful methods for dealing with versification
 * 
 * @author chrisburrell
 * 
 */
public interface JSwordVersificationService {

    /**
     * Uses the default versification to return the verse range name
     * 
     * @param startVerseId the starting verse ordinal
     * @param endVerseId the ending verse ordinal
     * @return the reference in human readable form
     */
    String getVerseRange(int startVerseId, int endVerseId);

    /**
     * Uses the version of the given book and calculates the verse range name
     * 
     * @param startVerseId the starting verse ordinal
     * @param endVerseId the ending verse ordinal
     * @param version the initials of the book
     * @return the reference in human readable form
     */
    String getVerseRange(int startVerseId, int endVerseId, String version);

    /**
     * Obtains a verse range for the provided verses
     * 
     * @param version the version to use for lookup purposes
     * @param numberedVersion the version of the Bible to use for the versification (i.e. verse ordinals)
     * @param versificationForNumberedVersion the actual versification object
     * @param s the start verse
     * @param e the end verse
     * @param lookupVersion the lookup version
     * @param roundReference whether to round up/down the reference
     * @param ignoreVerse0 whether to ignore verse 0 (i.e. the beginning of the chapter
     * @return the verse range in question
     */
    VerseRange getVerseRangeForSelectedVerses(String version, String numberedVersion,
            Versification versificationForNumberedVersion, Verse s, Verse e, Book lookupVersion,
            Boolean roundReference, boolean ignoreVerse0);

    /**
     * A helper method to get the versification from a book
     * 
     * @param version the version we are interested in.
     * @return the versification the versification of the book
     */
    Versification getVersificationForVersion(Book version);

    /**
     * A helper method to get the versification from a book
     * 
     * @param version the version we are interested in.
     * @return the versification the versification of the book
     */
    Versification getVersificationForVersion(String version);

    /**
     * Gets a book from version initials
     * 
     * @param version the version initials
     * @return the JSword book
     */
    Book getBookFromVersion(String version);
}