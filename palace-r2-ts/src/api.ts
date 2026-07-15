/**
 * The API exposed to the native application.
 */

export interface SR2APIType {
  /**
   * Highlight search terms.
   */

  highlightSearchingTerms(
    searchingTerms: string,
    clearHighlight: boolean,
  ): void;

  /**
   * Turn the page left (go to a previous page or chapter)
   */

  turnPageLeft(): void;

  /**
   * Turn the page right (go to a next page or chapter)
   */

  turnPageRight(): void;

  /**
   * Go to a specific position in the chapter.
   */

  goToPosition(offset: number): void;

  /**
   * Go to an element with the given ID.
   */

  goToId(id: string): void;

  /**
   * Set a document property.
   */

  setProperty(name: string, value: string): void;

  /**
   * Remove a document property.
   */

  removeProperty(name: string): void;
}
