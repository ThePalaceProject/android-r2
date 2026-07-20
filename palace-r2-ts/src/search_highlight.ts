export interface HighlightRange {
  start: number;
  length: number;
}

export function highlightSearchingTerms(
  searchingTerms: string,
  clearHighlight: boolean,
): boolean {
  if (!document.body || typeof document.body.innerHTML === 'undefined') {
    return false;
  }

  const bodyText = document.body.innerHTML;
  document.body.innerHTML = doHighlight(
    bodyText,
    searchingTerms,
    clearHighlight,
  );
  return true;
}

function doHighlight(
  bodyText: string,
  searchingTerms: string,
  clearHighlight: boolean,
): string {
  let newText = '';
  let i = -1;
  const lcSearchingTerm = searchingTerms.toLowerCase();
  let lcBodyText = bodyText.toLowerCase();

  const highlightStartTag = '<font style="background-color:yellow;">';
  const highlightEndTag = '</font>';

  while (bodyText.length > 0) {
    i = lcBodyText.indexOf(lcSearchingTerm, i + 1);
    if (i < 0) {
      newText += bodyText;
      break;
    }

    // Skip anything inside an HTML tag
    if (bodyText.lastIndexOf('>', i) >= bodyText.lastIndexOf('<', i)) {
      // Skip anything inside a <script> block
      if (
        lcBodyText.lastIndexOf('/script>', i) >=
        lcBodyText.lastIndexOf('<script', i)
      ) {
        let indexHighlightStartTag: number;
        let indexHighlightEndTag: number;

        if (clearHighlight) {
          indexHighlightStartTag = bodyText.indexOf(highlightStartTag);
          indexHighlightEndTag = bodyText.indexOf(highlightEndTag);
        } else {
          indexHighlightStartTag = -1;
          indexHighlightEndTag = -1;
        }

        if (indexHighlightStartTag !== -1 && indexHighlightEndTag !== -1) {
          newText +=
            bodyText.substring(0, indexHighlightStartTag) +
            bodyText.substr(i, searchingTerms.length);
          bodyText = bodyText.substring(
            indexHighlightEndTag + highlightEndTag.length,
          );
        } else {
          newText +=
            bodyText.substring(0, i) +
            highlightStartTag +
            bodyText.substr(i, searchingTerms.length) +
            highlightEndTag;
          bodyText = bodyText.substring(i + searchingTerms.length);
        }
        lcBodyText = bodyText.toLowerCase();
        i = -1;
      }
    }
  }

  return newText;
}
