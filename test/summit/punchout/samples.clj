(ns summit.punchout.samples)

(def punchout-test-str
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd\">
<cXML payloadID=\"the-payload\" timestamp=\"Mon May 19 18:29:48 +0000 2008\" xml:lang=\"en-US\">
  <Header>
    <From>
      <Credential domain=\"DUNS\">
        <Identity>from-company</Identity>
      </Credential>
    </From>
    <To>
      <Credential domain=\"DUNS\">
        <Identity>to-summit</Identity>
      </Credential>
    </To>
    <Sender>
      <Credential domain=\"DUNS\">
        <Identity>Sender</Identity>
        <SharedSecret>super-secret</SharedSecret>
      </Credential>
      <UserAgent>myagent</UserAgent>
    </Sender>
  </Header>
  <Request>
    <PunchOutSetupRequest operation=\"create\">
      <BuyerCookie>cookie-monster</BuyerCookie>
      <Extrinsic name=\"FirstName\">myfirstname</Extrinsic>
      <Extrinsic name=\"LastName\">mylastname</Extrinsic>
      <Extrinsic name=\"UniqueName\">myuniquename</Extrinsic>
      <Extrinsic name=\"UserEmail\">useremail</Extrinsic>
      <Extrinsic name=\"User\">myuser</Extrinsic>
      <Extrinsic name=\"BusinessUnit\">mybusinessunit</Extrinsic>
      <BrowserFormPost>
        <URL>https://broker.com/punchout/checkout/1</URL>
      </BrowserFormPost>
      <Contact role=\"myrole\">
        <Name xml:lang=\"en-US\">jim</Name>
        <Email>contactemail</Email>
      </Contact>
    </PunchOutSetupRequest>
  </Request>
</cXML>")

