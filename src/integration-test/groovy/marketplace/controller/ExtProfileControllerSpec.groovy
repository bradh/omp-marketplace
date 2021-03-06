package marketplace.controller

import grails.converters.JSON
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import marketplace.Category
import marketplace.Constants
import marketplace.CustomFieldDefinition
import marketplace.ExtProfile
import marketplace.ExtProfileController
import marketplace.Profile
import marketplace.SecurityMixin
import marketplace.State
import marketplace.Types
import marketplace.controller.ControllerTestMixin
import marketplace.testutil.FakeAuditTrailHelper
import org.grails.web.json.JSONObject
import spock.lang.Specification

@Integration
@Rollback
class ExtProfileControllerSpec extends MarketplaceIntegrationTestCase implements ControllerTestMixin<ExtProfileController> {
	def sessionFactory
	def extProfileService
	def JSONDecoratorService
	private def svcProps

    private def svcProps2

	void setupData() {
		setupTestUser()
		sessionFactory.currentSession.flush()
		sessionFactory.currentSession.clear()
		controller.extProfileService = extProfileService

		// Make sure referenced objects exist in database first
		def author = new Profile(username:"junitAuth").save(failOnError: true)
		def types = new Types(title:"Service:JUnit").save(failOnError: true)
		def state = new State(title:"JUnit").save(failOnError: true)
		def cat1 = new Category(title:"JUnit Cat 1").save(failOnError: true)
		def cat2 = new Category(title:"JUnit Cat 2").save(failOnError: true)
		def cdf1 = new CustomFieldDefinition(
            label: 'lbl',
            name:"JUnit Custom 1",
            styleType: Constants.CustomFieldDefinitionStyleType.TEXT
        ).save(failOnError: true)
		def cdf2 = new CustomFieldDefinition(
            label: 'lbl',
            name:"JUnit Custom 2",
            styleType: Constants.CustomFieldDefinitionStyleType.TEXT
        ).save(failOnError: true)

		//TODO BVEST Come back
        switchBothAdmin(author.username)

		svcProps = [
          username: "jmontana",
	      displayName: "joe the man",
	      email: "jmontana@yahoo.com",
	      bio: "Went to high school.",
	      systemUri: "external:system",
	      externalId: "jmontana1234",
	      externalViewUrl: "http://www.joe_montana.com",
	      externalEditUrl: "http://www.joe_montana.com/edit"
     ]

    svcProps2 = [
          username: "jrice",
	      displayName: "jerry the man",
	      email: "jrice@yahoo.com",
	      bio: "Went to college.",
	      //createdDate: "2010-10-27T16:06:07Z",
	      systemUri: "external:system",
	      externalId: "jrice1234",
	      externalViewUrl: "http://www.jerry_arroz.com",
	      externalEditUrl: "http://www.jerry_arroz.com/edit"
     ]
	}

    // Until I can get logging from the test to show up in the log file - Charlie
    def logIt(def strIn)
    {
        controller.logIt(strIn)
    }

	def createJSON(def jsonProperty, def jsonPropertyProperties){
		def jsonReq = new JSONObject(
				"${jsonProperty}": new JSONObject(jsonPropertyProperties)
		)
		JSONDecoratorService.postProcessJSON(jsonReq)
		return jsonReq as JSON
	}


	public void testCreate(){
		setupData()
        logIt "running testCreate:"
		//1.times {
		when:
			def jsonReq = createJSON("profile", svcProps)

			controller.request.contentType = "text/json"
			controller.request.content=jsonReq.toString()
			controller.session.username=svcProps.author

			controller.create()
		then:
			201 == controller.response.status

		when:
			def respStr=controller.response.contentAsString
			def jsonResp = JSON.parse(respStr)
			def profileId = jsonResp.id

		then:
			null != profileId

		when:
			// Check to make sure we now have a new ExtProfile in the db
			def extProfile = Profile.get(profileId)

		then:
			null != extProfile

            svcProps.username ==extProfile.username
            svcProps.displayName ==extProfile.displayName
            svcProps.email ==extProfile.email
            svcProps.bio ==extProfile.bio
            //createdDate: "2010-10-27T16:06:07Z",
            svcProps.systemUri ==extProfile.systemUri
            svcProps.externalId ==extProfile.externalId
            svcProps.externalViewUrl ==extProfile.externalViewUrl
            svcProps.externalEditUrl ==extProfile.externalEditUrl
		//}
	}


    // Set launchUrl to an empty string which should cause the create to fail.
	public void testCreateWithBadData(){
		setupData()
		when:
        logIt "running testCreateWithBadData:"
		svcProps.username=""
		def jsonReq = createJSON("profile", svcProps)

		controller.request.contentType = "text/json"
		controller.request.content=jsonReq.toString()
		controller.session.username=svcProps.author

		controller.create()

		then:
		400 == controller.response.status
	}

	public void testCreateWithNoData(){
		setupData()
		when:
		logIt "running testCreateWithNoData:"
		controller.request.contentType = "text/json"
		controller.request.content=""
		controller.session.username=svcProps.author

		controller.create()

		then:
        //in grails 1 this was a 500, but 400 does make
        //more sense.  Is it OK that it changed?
		400 == controller.response.status
	}

	public void testUpdate(){
		setupData()
		when:
		controller.logIt("running testUpdate:")
		def extProfile = newProfile('atYourService ABC', 'jrice', 'extjrice')
        def author1 = new Profile(username:"mick jagger")

		then:
        'jrice' ==extProfile.username
        svcProps2.displayName ==extProfile.displayName
        svcProps2.email ==extProfile.email
        svcProps2.bio ==extProfile.bio
        //createdDate: "2010-10-27T16:06:07Z",
        'atYourService ABC' ==extProfile.systemUri
        'extjrice' ==extProfile.externalId
        svcProps2.externalViewUrl ==extProfile.externalViewUrl
        svcProps2.externalEditUrl ==extProfile.externalEditUrl

		when:
		def jsonReq = createJSON("profile", svcProps)

		controller.params.id = extProfile.id
		controller.request.contentType = "text/json"
		controller.request.content=jsonReq.toString()
		controller.session.username=author1.username

		controller.update()

		then:
		200 == controller.response.status

		when:
		// Check values in database
		extProfile = Profile.get(extProfile.id)
        then:
		svcProps.username ==extProfile.username
        svcProps.displayName ==extProfile.displayName
        svcProps.email ==extProfile.email
        svcProps.bio ==extProfile.bio
        //createdDate: "2010-10-27T16:06:07Z",
        svcProps.systemUri ==extProfile.systemUri
        svcProps.externalId ==extProfile.externalId
        svcProps.externalViewUrl ==extProfile.externalViewUrl
        svcProps.externalEditUrl ==extProfile.externalEditUrl
	}

	// Try an update that violates the unique constraint on username.
	public void testUpdateWithBadData(){
		setupData()
		when:
		controller.logIt "running testUpdateWithBadData:"
		def profile = newProfile('ext8', "Bill Withers", 'extbw')
        def profile2 = newProfile('ext9', "Mr. Smithers", 'extmrsmith')
        //def author1 = new Profile(username:"mick jagger")
        //extSvc.createdBy = author1.id

		def jsonReq = createJSON("profile", [username:"Mr. Smithers"])

		controller.params.id = profile.id
		controller.request.contentType = "text/json"
		controller.request.content=jsonReq.toString()
		//controller.session.username=author1.username

		controller.update()

		then:
		400 == controller.response.status
	}


	public void testRetrieveById(){
		setupData()
		when:
        controller.logIt "running testRetrieveById:"
		def ext4 = "external:system4"
		def profiles = []
		4.times{
			def profile = newProfile(ext4, "Henry the ${it}", "EXTID${it}")
			profiles.add profile
		}

		controller.params.id=profiles[0].id
		controller.session.username=svcProps.author
		controller.getItemAsJSON()
		then:
		200 == controller.response.status

		when:
		def respStr=controller.response.contentAsString
        controller.logIt "respStr = ${respStr}"

		// Check response JSON, make sure expected values are set
		def jsonResp = JSON.parse(respStr)

		then:
		1 == jsonResp?.total
		profiles[0].id == (jsonResp?.data.id as Integer)
	}

    public void testRetrieveAll(){
		setupData()
		when:
        controller.logIt "running testRetrieveByUsername:"
		def ext4 = "external:system4"
		def profiles = []
        def profile = newProfile(ext4, "rcraig", "craig007")
        profiles.add profile
		4.times{
			profile = newProfile(ext4, "Henry the ${it}", "EXTID${it}")
			profiles.add profile
		}

		controller.session.username=svcProps.author
		controller.getListAsJSON()
		then:
		200 == controller.response.status

		when:
		def respStr=controller.response.contentAsString
        controller.logIt "respStr = ${respStr}"

		// Check response JSON, make sure expected values are set
		def jsonResp = JSON.parse(respStr)
		then:
		5 == jsonResp?.total
        5 == (jsonResp?.data.size())
	}

    public void testRetrieveBySystemUriAndExternalId(){
		setupData()
		when:
        controller.logIt "running testRetrieveByUsername:"
		def ext4 = "external:system4"
		def profiles = []
        def profile = newProfile(ext4, "rcraig", "craig007")
        profiles.add profile
		4.times{
			profile = newProfile(ext4, "Henry the ${it}", "EXTID${it}")
			profiles.add profile
		}

		controller.params.systemUri=ext4
        controller.params.externalId='craig007'
		controller.session.username=svcProps.author
		controller.getListAsJSON()
		then:
		200 == controller.response.status

		when:
		def respStr=controller.response.contentAsString
        controller.logIt "respStr = ${respStr}"

		// Check response JSON, make sure expected values are set
		def jsonResp = JSON.parse(respStr)
		controller.logIt "classname = ${jsonResp?.data.getClass()}"
		then:
		1 == jsonResp?.total
		'rcraig' == (jsonResp?.data[0].username)
		1 == (jsonResp?.data.size())
	}

    public void testRetrieveByUsername(){
		setupData()
		when:
        controller.logIt "running testRetrieveByUsername:"
		def ext4 = "external:system4"
		def profiles = []
        def profile = newProfile(ext4, "rcraig", "craig007")
        profiles.add profile
		4.times{
			profile = newProfile(ext4, "Henry the ${it}", "EXTID${it}")
			profiles.add profile
		}

		controller.params.username='rcraig'
		controller.session.username=svcProps.author
		controller.getListAsJSON()
		then:
		200 == controller.response.status

		when:
		def respStr=controller.response.contentAsString
        controller.logIt "respStr = ${respStr}"

		// Check response JSON, make sure expected values are set
		def jsonResp = JSON.parse(respStr)
		controller.logIt "classname = ${jsonResp?.data.getClass()}"
		then:
		1 == jsonResp?.total
		'rcraig' == (jsonResp?.data[0].username)
        1 == (jsonResp?.data.size())
	}

    public void testRetrieveByUsernameNotFound(){
		setupData()
		when:
        controller.logIt "running testRetrieveByUsername:"
		def ext4 = "external:system4"
		def profiles = []
        def profile = newProfile(ext4, "rcraig", "craig007")
        profiles.add profile
		4.times{
			profile = newProfile(ext4, "Henry the ${it}", "EXTID${it}")
			profiles.add profile
		}

		controller.params.username='rcrag'
		controller.session.username=svcProps.author
		controller.getListAsJSON()
		then:
		200 == controller.response.status

		when:
		def respStr=controller.response.contentAsString
        controller.logIt "respStr = ${respStr}"

		// Check response JSON, make sure expected values are set
		def jsonResp = JSON.parse(respStr)
		then:
		0 == jsonResp?.total
		0 == (jsonResp?.data.size())
	}

	public void testRetrieveByIdNotFound(){
		setupData()
		when:
        controller.logIt "running testRetrieveByIdNotFound:"
        def id = 2041

		controller.params.id=id
		controller.session.username=svcProps.author
		controller.getItemAsJSON()
		then:
		404 == controller.response.status
	}

	private newProfile(def systemUri, def username, def externalId){
		def extProfile = new ExtProfile(svcProps2)
        extProfile.username = username
        extProfile.systemUri = systemUri
        extProfile.externalId = externalId
		if (!extProfile.save(flush: true))
        {
            extProfile.errors.each{
                logIt "Error on save: ${it}"
            }
        }

		return extProfile
	}

}
