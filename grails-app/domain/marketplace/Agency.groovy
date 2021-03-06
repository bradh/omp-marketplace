package marketplace

import org.grails.web.json.JSONObject


class Agency extends AuditStamped implements Serializable, ToJSON {
    final static bindableProperties = ['title', 'iconUrl']
    final static modifiableReferenceProperties = []

	//For searching
    static searchable = {
        root false
        title index: 'analyzed', excludeFromAll: false
        iconUrl index: 'not_analyzed', excludeFromAll: true
        only = ['id', 'title', 'iconUrl']
    }

    static constraints = {
        title(blank: false, nullable:false, maxSize:255, validator: { val, obj ->
            def agency = Agency.findByTitleIlike(val)
            return (!agency || obj.id == agency.id) ? true : ['unique']
        })
        iconUrl(blank: false, nullable:false, maxSize:2000, unique: false)
		agencyId(nullable: true) //	//For searching
    }

	//For searching
	static transients = ['agencyId', 'description']

    String title
    String iconUrl
    //long parentId

    @Override
    JSONObject asJSON() {
        marshall([id     : this.id,
                  title  : this.title,
                  iconUrl: this.iconUrl])
    }

	//For searching
	Long getAgencyId() {
		id
	}

	//For searching
	String toString() { "$id : $title" }


	String getDescription(){
		null
	}

    boolean equals(other) {
        other instanceof Agency && this.title == other.title
    }

    int hashCode() {
        title?.hashCode() ?: 0
    }

    /**
     * This method is used by the import logic to create a Domain object.
     */
    void bindFromJSON(JSONObject obj) {
        this.with {
            title = obj.title
            iconUrl = obj.iconUrl
        }
    }
}
