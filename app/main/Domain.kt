internal object Domain {
    fun from(brev: SanityModel.Brevmal): Brevmal = Brevmal.from(brev)

    data class Brevmal(
        val id: String,
        val tittel: String,
        val systeminnhold: List<Systeminnhold>?,
        val standardtekster: List<Standardtekst>?,
    ) {
        companion object {
            fun from(brev: SanityModel.Brevmal): Brevmal {
                return Brevmal(
                    id = brev._id,
                    tittel = brev.brevtittel,
                    systeminnhold = brev.innhold?.mapNotNull(Systeminnhold::from),
                    standardtekster = brev.innhold?.mapNotNull(Standardtekst::from),
                )
            }
        }
    }

    data class Systeminnhold(
        val id: String,
        val systemnøkkel: String,
    ) {
        companion object {
            fun from(innhold: SanityModel.Innhold): Systeminnhold? {
                if (innhold._type != SanityModel.Innhold.Type.systeminnhold) {
                    return null
                }

                return Systeminnhold(
                    id = innhold._id,
                    systemnøkkel = requireNotNull(innhold.systemNokkel),
                )
            }
        }
    }

    data class Standardtekst(
        val id: String,
        val innhold: List<TextContent>,
        val overskriftsnivå: Header?,
        val overskrift: String?,
        val redigerbar: Boolean,
        val hjelpetekst: List<TextContent>,
    ) {
        companion object {
            fun from(innhold: SanityModel.Innhold): Standardtekst? {
                if (innhold._type != SanityModel.Innhold.Type.standardtekst) {
                    return null
                }

                return Standardtekst(
                    id = innhold._id,
                    innhold = innhold.innhold?.map(TextContent::from) ?: emptyList(),
                    overskriftsnivå = innhold.niva?.let { Header.valueOf(it.name) },
                    overskrift = innhold.overskrift,
                    redigerbar = innhold.kanRedigeres ?: false,
                    hjelpetekst = innhold.hjelpetekst?.map(TextContent::from) ?: emptyList(),
                )
            }
        }
    }

    enum class Header { H1, H2, H3 }

    data class TextContent(
        val spans: List<Span>,
        val inlineElements: List<InlineElement>,
        val systemVariables: List<SystemVariable>,
        val listItem: String?,
        val style: String?, // normal
        val level: Int,
    ) {
        companion object {
            fun from(text: SanityModel.PortableText): TextContent {
                if (text._type != SanityModel.PortableText.Type.content) {
                    PUBLIC_LOGGER.warn(
                        """
                            standardtekst.innhold.type was ${text._type} and has no mapper on its own, 
                            reuses mapper of type 'content'.
                        """.trimIndent()
                    )
                }

                return TextContent(
                    spans = text.children.mapNotNull(Span::from),
                    inlineElements = text.children.mapNotNull(InlineElement::from),
                    systemVariables = text.children.mapNotNull(SystemVariable::from),
                    listItem = text.listItem,
                    style = text.style?.name,
                    level = text.level ?: -1,
                )
            }
        }
    }

    enum class Mark { bold, italic, underline }

    data class Span(
        val text: String,
        val marks: List<Mark>,
    ) {
        companion object {
            fun from(child: SanityModel.PortableText.Child): Span? {
                if (child._type != SanityModel.PortableText.Child.Type.span) {
                    return null
                }

                return Span(
                    text = child.text!!,
                    marks = child.marks?.map {
                        when (it) {
                            SanityModel.PortableText.Child.Mark.strong -> Mark.bold
                            SanityModel.PortableText.Child.Mark.em -> Mark.italic
                            SanityModel.PortableText.Child.Mark.underline -> Mark.underline
                        }
                    } ?: emptyList(),
                )
            }
        }
    }

    data class InlineElement(
        val text: String,
    ) {
        companion object {
            fun from(child: SanityModel.PortableText.Child): InlineElement? {
                if (child._type != SanityModel.PortableText.Child.Type.inlineElement) {
                    return null
                }

                return InlineElement(
                    text = child.text!!,
                )
            }
        }
    }

    data class SystemVariable(
        val _ref: String,
        val systemVariabel: String,
    ) {
        companion object {
            fun from(child: SanityModel.PortableText.Child): SystemVariable? {
                if (child._type != SanityModel.PortableText.Child.Type.systemVariabel) {
                    return null
                }

                return SystemVariable(
                    _ref = child._ref!!,
                    systemVariabel = child.systemVariabel!!,
                )
            }
        }
    }

}
