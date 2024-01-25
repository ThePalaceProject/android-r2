package org.librarysimplified.r2.vanilla.internal

import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.data.ReadTry
import org.readium.r2.shared.util.pdf.PdfDocument
import org.readium.r2.shared.util.pdf.PdfDocumentFactory
import org.readium.r2.shared.util.resource.Resource
import kotlin.reflect.KClass

/**
 * A PDF factory that always fails.
 */

object SR2NoPDFFactory : PdfDocumentFactory<PdfDocument> {

  override val documentType: KClass<PdfDocument>
    get() = PdfDocument::class

  override suspend fun open(resource: Resource, password: String?): ReadTry<PdfDocument> {
    return ReadTry.failure(ReadError.UnsupportedOperation("Not supported!"))
  }
}
