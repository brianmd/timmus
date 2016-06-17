(ns timmus.routes.fake-axiall
  ;; (:require ;; [clojure.test :as t]
  ;;  )
  )


(defn fake-axiall-punchout-request []
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd\">
<cXML payloadID=\"1211221788.71299@ip-10-251-122-83\" timestamp=\"Mon May 19 18:29:48 +0000 2008\" xml:lang=\"en-US\">
  <Header>
    <From>
      <Credential domain=\"DUNS\">
        <Identity>coupa-t</Identity>
      </Credential>
    </From>
    <To>
      <Credential domain=\"DUNS\">
        <Identity>coupa-t</Identity>
      </Credential>
    </To>
    <Sender>
      <Credential domain=\"DUNS\">
        <Identity>axiall</Identity>
        <SharedSecret>wi7oie.c</SharedSecret>
      </Credential>
      <UserAgent>myagent</UserAgent>
    </Sender>
  </Header>
  <Request>
    <PunchOutSetupRequest operation=\"create\">
      <BuyerCookie>c64af92dc27e68172e030d3d</BuyerCookie>
        <Extrinsic name=\"FirstName\">myfirstname</Extrinsic>
      <Extrinsic name=\"LastName\">mylastname</Extrinsic>
      <Extrinsic name=\"UniqueName\">myuniquename</Extrinsic>
      <Extrinsic name=\"UserEmail\">myemail</Extrinsic>
      <Extrinsic name=\"User\">myuser</Extrinsic>
      <Extrinsic name=\"BusinessUnit\">mybusinessunit</Extrinsic>
      <BrowserFormPost>
        <URL>http://localhost:3449/api/punchout/accept-order-message/4</URL>
      </BrowserFormPost>
      <Contact role=\"myrole\">
        <Name xml:lang=\"en-US\">jim</Name>
        <Email>myemail</Email>
      </Contact>
    </PunchOutSetupRequest>
  </Request>
</cXML>"
  )
