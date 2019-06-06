package nl.rubensten.texifyidea.settings

data class LabelingCommandInformation(var commandName: String, var position: Int, var labelsPreviousCommand: Boolean) {

    companion object {
    
        /**
         * Needed to get back the object from the string in the xml file
         */
        @JvmStatic fun fromString(string: CharSequence): LabelingCommandInformation {
            val parts = string.split(";")
            require (parts.size == 3) {
                "String must contain exactly three parts, got ${parts.size}"
            }
            val position = parts[1].toIntOrNull() ?: error("Second part must be type Int")
            val labelsPreviousCommand = parts[2].toBoolean()
            return LabelingCommandInformation(parts.first(), position, labelsPreviousCommand)
        }
    }

    /**
     * Needed to save the information to the xml file
     */
    fun toSerializableString() = "$commandName;$position;$labelsPreviousCommand"
}
