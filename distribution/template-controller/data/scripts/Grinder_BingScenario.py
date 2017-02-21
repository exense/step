# The Grinder 3.11
# HTTP script recorded by TCPProxy at 2 dec. 2016 08:54:39

from net.grinder.script import Test
from net.grinder.plugin.http import HTTPPluginControl, HTTPRequest, FakeGrinderObject
from HTTPClient import NVPair, DefaultAuthHandler, CookieModule
from java.lang import System as javasystem
connectionDefaults = HTTPPluginControl.getConnectionDefaults()
httpUtilities = HTTPPluginControl.getHTTPUtilities()

# Stuff you have to add to be able to use grinder-proxy-generated scripts (for quick compatibility)
grinder = FakeGrinderObject()

# Load or unload the modules you want, set handlers the handlers you want
modules = "HTTPClient.RetryModule|HTTPClient.CookieModule|HTTPClient.RedirectionModule|HTTPClient.AuthorizationModule|HTTPClient.DefaultModule|HTTPClient.TransferEncodingModule|HTTPClient.ContentMD5Module|HTTPClient.ContentEncodingModule"
javasystem.setProperty("HTTPClient.Modules", modules)
CookieModule.setCookiePolicyHandler(None);
DefaultAuthHandler.setAuthorizationPrompter(None);

# To use a proxy server, uncomment the next line and set the host and port.
# connectionDefaults.setProxyServer("localhost", 8001)

def createRequest(test, url, headers=None):
    """Create an instrumented HTTPRequest."""
    request = HTTPRequest(url=url)
    if headers: request.headers=headers
    test.record(request, HTTPRequest.getHttpMethodFilter())
    return request

# These definitions at the top level of the file are evaluated once,
# when the worker process is started.

connectionDefaults.defaultHeaders = \
  [ NVPair('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0'),
    NVPair('Accept-Encoding', 'gzip, deflate'),
    NVPair('Accept-Language', 'fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3'), ]

headers0= \
  [ NVPair('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'), ]

headers1= \
  [ NVPair('Accept', '*/*'),
    NVPair('Referer', 'http://www.bing.com/'), ]

headers2= \
  [ NVPair('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'),
    NVPair('Referer', 'http://www.bing.com/'), ]

headers3= \
  [ NVPair('Accept', '*/*'),
    NVPair('Referer', 'http://www.bing.com/search?q=TOTOLASTICOT&qs=n&form=QBLH&pq=totolastico&sc=0-11&sp=-1&sk=&cvid=23F1BE72181943578303D954BED4EBDB'), ]

headers4= \
  [ NVPair('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'),
    NVPair('Referer', 'http://www.bing.com/search?q=TOTOLASTICOT&qs=n&form=QBLH&pq=totolastico&sc=0-11&sp=-1&sk=&cvid=23F1BE72181943578303D954BED4EBDB'), ]

url0 = 'http://bing.com:80'
url1 = 'http://www.bing.com:80'
url2 = 'http://a4.bing.com:80'
url3 = 'http://s2.symcb.com:80'
url4 = 'http://ocsp.digicert.com:80'
url5 = 'http://d7f441b6532f6652359d9ca3709aa48f.clo.footprintdns.com:80'
url6 = 'http://1f4050437360130bc63cd6a0a3ed4d39.clo.footprintdns.com:80'
url7 = 'http://3ec0094a75dafe6b204eb76d15bea609.clo.footprintdns.com:80'
url8 = 'http://2.bing.com:80'
url9 = 'http://report.footprintdns.com:80'


# AllerSurBing_debut
request101 = createRequest(Test(101, 'GET /'), url0, headers0)

request201 = createRequest(Test(201, 'GET /'), url1, headers0)

request202 = createRequest(Test(202, 'GET hpc18.png'), url1, headers1)

request203 = createRequest(Test(203, 'GET bing_p_rr_teal_min.ico'), url1, headers0)

request301 = createRequest(Test(301, 'GET l'), url1, headers1)

request401 = createRequest(Test(401, 'POST lsp.aspx'), url1, headers2)

request402 = createRequest(Test(402, 'GET 8665a969.js'), url1, headers1)

request403 = createRequest(Test(403, 'GET 3adc8d70.js'), url1, headers2)

request404 = createRequest(Test(404, 'GET f1d86b5a.js'), url1, headers2)

request405 = createRequest(Test(405, 'GET 89faaefc.js'), url1, headers2)

request406 = createRequest(Test(406, 'GET 015d6a32.js'), url1, headers2)

request407 = createRequest(Test(407, 'GET ResurrectionBay_FR-FR9938760197_1920x1080.jpg'), url1, headers1)

request408 = createRequest(Test(408, 'GET d0c1edfd.js'), url1, headers1)

request409 = createRequest(Test(409, 'GET HPImgVidViewer_c.js'), url1, headers1)

request501 = createRequest(Test(501, 'GET HPImageArchive.aspx'), url1, headers2)

request502 = createRequest(Test(502, 'GET HpbCarouselHeaderPopup.js'), url1, headers1)

request601 = createRequest(Test(601, 'GET render'), url1, headers2)

request701 = createRequest(Test(701, 'GET hpm'), url1, headers2)

request702 = createRequest(Test(702, 'GET 409a194b.png'), url1, headers1)

request801 = createRequest(Test(801, 'GET th'), url1, headers1)

request901 = createRequest(Test(901, 'GET th'), url1, headers1)

request1001 = createRequest(Test(1001, 'GET th'), url1, headers1)

request1101 = createRequest(Test(1101, 'GET th'), url1, headers1)

request1201 = createRequest(Test(1201, 'GET l'), url1, headers1)

request1301 = createRequest(Test(1301, 'GET th'), url1, headers1)

request1401 = createRequest(Test(1401, 'GET th'), url1, headers1)

request1501 = createRequest(Test(1501, 'GET th'), url1, headers1)

request1601 = createRequest(Test(1601, 'GET th'), url1, headers1)

request1701 = createRequest(Test(1701, 'GET th'), url1, headers1)

request1801 = createRequest(Test(1801, 'GET th'), url1, headers1)

request1901 = createRequest(Test(1901, 'GET th'), url1, headers1)

request2001 = createRequest(Test(2001, 'GET th'), url1, headers1)

request2101 = createRequest(Test(2101, 'GET th'), url1, headers1)

request2201 = createRequest(Test(2201, 'GET th'), url1, headers1)

request2301 = createRequest(Test(2301, 'GET th'), url1, headers1)

request2401 = createRequest(Test(2401, 'GET th'), url1, headers1)

request2402 = createRequest(Test(2402, 'GET RedGrouseScotland_FR-FR13362947184_1920x1080.jpg'), url1, headers1)

request2501 = createRequest(Test(2501, 'POST lsp.aspx'), url1, headers2)

request2601 = createRequest(Test(2601, 'GET l'), url2, headers1)

request2701 = createRequest(Test(2701, 'GET th'), url1, headers1)

request2801 = createRequest(Test(2801, 'GET th'), url1, headers1)

request2901 = createRequest(Test(2901, 'GET th'), url1, headers1)

# //FinBing
request3001 = createRequest(Test(3001, 'GET Passport.aspx'), url1, headers2)

# //MotCleTOTOLASTICOT
request3101 = createRequest(Test(3101, 'GET Suggestions'), url1, headers2)

request3201 = createRequest(Test(3201, 'GET Suggestions'), url1, headers2)

request3301 = createRequest(Test(3301, 'GET Suggestions'), url1, headers2)

request3401 = createRequest(Test(3401, 'GET Suggestions'), url1, headers2)

request3501 = createRequest(Test(3501, 'GET Suggestions'), url1, headers2)

request3601 = createRequest(Test(3601, 'GET th'), url1, headers1)

request3701 = createRequest(Test(3701, 'GET Suggestions'), url1, headers2)

request3801 = createRequest(Test(3801, 'GET Suggestions'), url1, headers2)

request3901 = createRequest(Test(3901, 'GET th'), url1, headers1)

request4001 = createRequest(Test(4001, 'GET Suggestions'), url1, headers2)

request4101 = createRequest(Test(4101, 'GET Suggestions'), url1, headers2)

request4201 = createRequest(Test(4201, 'GET Suggestions'), url1, headers2)

request4301 = createRequest(Test(4301, 'GET Suggestions'), url1, headers2)

request4401 = createRequest(Test(4401, 'GET Suggestions'), url1, headers2)

request4501 = createRequest(Test(4501, 'POST lsp.aspx'), url1, headers2)

request4601 = createRequest(Test(4601, 'POST lsp.aspx'), url1, headers2)

request4701 = createRequest(Test(4701, 'GET search'), url1, headers2)

request4702 = createRequest(Test(4702, 'GET sw_nh_smallid_hamleft_accstar.png'), url1, headers3)

request4801 = createRequest(Test(4801, 'GET l'), url1, headers3)

request4901 = createRequest(Test(4901, 'POST lsp.aspx'), url1, headers4)

request4902 = createRequest(Test(4902, 'GET d8562e0f.js'), url1, headers4)

request4903 = createRequest(Test(4903, 'GET f0e4bfe8.js'), url1, headers4)

request4904 = createRequest(Test(4904, 'GET 640ee89b.js'), url1, headers4)

request4905 = createRequest(Test(4905, 'GET 68b0925c.js'), url1, headers4)

request5001 = createRequest(Test(5001, 'POST /'), url4, headers0)

request5101 = createRequest(Test(5101, 'POST lsp.aspx'), url1, headers4)

request5201 = createRequest(Test(5201, 'GET trans.gif'), url5, headers3)

request5301 = createRequest(Test(5301, 'GET trans.gif'), url6, headers3)

request5401 = createRequest(Test(5401, 'GET trans.gif'), url7, headers3)

request5501 = createRequest(Test(5501, 'GET 17k.gif'), url6, headers3)

request5601 = createRequest(Test(5601, 'GET 17k.gif'), url7, headers3)

request5701 = createRequest(Test(5701, 'GET 17k.gif'), url5, headers3)

request5801 = createRequest(Test(5801, 'GET Passport.aspx'), url1, headers4)

request5901 = createRequest(Test(5901, 'GET test'), url8, headers3)

request6001 = createRequest(Test(6001, 'GET trans.gif'), url9, headers3)


class TestRunner:
  """A TestRunner instance is created for each worker thread."""

  # A method for each recorded page.
  def page1(self):
    """GET / (request 101)."""

    # Expecting 301 'Moved Permanently'
    result = request101.GET('/')

    return result

  def page2(self):
    """GET / (requests 201-203)."""
    result = request201.GET('/')

    grinder.sleep(49)
    request202.GET('/s/a/hpc18.png')

    grinder.sleep(13)
    request203.GET('/sa/simg/bing_p_rr_teal_min.ico')

    return result

  def page3(self):
    """GET l (request 301)."""
    self.token_IG = \
      '23F1BE72181943578303D954BED4EBDB'
    self.token_CID = \
      '14AFED192DF6609C120BE4C62C7D619B'
    self.token_Type = \
      'Event.CPT'
    self.token_DATA = \
      '{\"pp\":{\"S\":\"L\",\"FC\":-1,\"BC\":-1,\"SE\":-1,\"TC\":-1,\"H\":23,\"BP\":47,\"CT\":58,\"IL\":1},\"ad\":[-1,-1,1166,685,1166,685,1]}'
    self.token_P = \
      'SERP'
    self.token_DA = \
      'DB5'
    result = request301.GET('/fd/ls/l' +
      '?IG=' +
      self.token_IG +
      '&CID=' +
      self.token_CID +
      '&Type=' +
      self.token_Type +
      '&DATA=' +
      self.token_DATA +
      '&P=' +
      self.token_P +
      '&DA=' +
      self.token_DA)

    return result

  def page4(self):
    """POST lsp.aspx (requests 401-409)."""
    result = request401.POST('/fd/ls/lsp.aspx',
      '<ClientInstRequest><Events><E><T>Event.ClientInst</T><IG>23F1BE72181943578303D954BED4EBDB</IG><TS>1480665314791</TS><D><![CDATA[{id:8506,P:\"2:2k,3:2k,4:2k,5:2k,7:2k,8:2n,9:5h,10:5v,11:5i,12:76,13:7d,14:7e,15:7n,16:7n,17:7o\",S:\"nav:0\",v:1.1,T:\"CI.Perf\",FID:\"CI\",Name:\"PerfV2\"}]]></D></E></Events><STS>1480665314791</STS></ClientInstRequest>',
      ( NVPair('Content-Type', 'text/plain;charset=UTF-8'), ))

    self.token_bu = \
      'rms+answers+Shared+BingCore$ClientInstV2$DuplicateXlsDefaultConfig,BingCore$ClientInstV2$SharedLocalStorageConfigDefault,BingCore$shared,BingCore$env.override,Empty,BingCore$event.custom.fix,BingCore$event.native,BingCore$onHTML,BingCore$dom,BingCore$cookies,BingCore$rmsajax,BingCore$ClientInstV2$LogUploadCapFeatureDisabled,BingCore$ClientInstV2$ClientInstConfigSeparateOfflineQueue,BingCore$clientinst,BingCore$replay,BingCore$Animation,BingCore$fadeAnimation,BingCore$framework'
    request402.GET('/rms/BingCore.Bundle/jc,nj/1f83a7b4/8665a969.js' +
      '?bu=' +
      self.token_bu)

    request403.GET('/rms/rms answers Identity Blue$BlueIdentityHeader/jc,nj/a3acd196/3adc8d70.js')

    #request404.GET('/rms/rms answers Identity SnrWindowsLiveConnectBootstrap/jc,nj/bf587ad6/f1d86b5a.js')
    request404.GET('/rms/rms answers Identity SnrWindowsLiveConnectBootstrap/jc,nj/bf587ad6/f1d86b5a.js')

    request405.GET('/rms/rms answers Identity Blue$BlueIdentityDropdownBootStrap/jc,nj/c0fac2c5/89faaefc.js')

    self.token_bu = \
      'rms+answers+BoxModel+config.instant,core,core$viewport,core$layout,core$metrics,modules$mutation,modules$error,modules$network,modules$cursor,modules$keyboard,modules$bot'
    request406.GET('/rms/Framework/jc,nj/63cb1f96/015d6a32.js' +
      '?bu=' +
      self.token_bu)

    grinder.sleep(50)
    request407.GET('/az/hprichbg/rb/ResurrectionBay_FR-FR9938760197_1920x1080.jpg')

    self.token_bu = \
      'rms+answers+AutoSuggest+Service,Web$Utils,Web$EventRegisterer,Web$EventRegistration,Web$History,Empty,Empty,Empty,Web$WebCore,Web$DataProvider,Empty,Empty,Web$Canvas,Web$Layout,Web$SearchForm,Web$Ghosting,Empty,Web$PrefixThrottling,Empty,Empty,Web$Init'
    request408.GET('/rms/AutoSug/jc,nj/55cdb87a/d0c1edfd.js' +
      '?bu=' +
      self.token_bu)

    request409.GET('/sa/8_1_2_5207728/HPImgVidViewer_c.js')

    return result

  def page5(self):
    """GET HPImageArchive.aspx (requests 501-502)."""
    self.token_format = \
      'js'
    self.token_idx = \
      '0'
    self.token_n = \
      '1'
    self.token_nc = \
      '1480665315010'
    self.token_pid = \
      'hp'
    self.token_video = \
      '1'
    result = request501.GET('/HPImageArchive.aspx' +
      '?format=' +
      self.token_format +
      '&idx=' +
      self.token_idx +
      '&n=' +
      self.token_n +
      '&nc=' +
      self.token_nc +
      '&pid=' +
      self.token_pid +
      '&video=' +
      self.token_video)

    request502.GET('/sa/8_1_2_5207728/HpbCarouselHeaderPopup.js')

    return result

  def page6(self):
    """GET render (request 601)."""
    self.token_bnptrigger = \
      '{\"PartnerId\":\"HomePage\",\"IID\":\"SERP.2000\",\"Attributes\":{\"RawRequestURL\":\"/\"}}'
    self.token_IID = \
      'SERP.2000'
    result = request601.GET('/notifications/render' +
      '?bnptrigger=' +
      self.token_bnptrigger +
      '&IG=' +
      self.token_IG +
      '&IID=' +
      self.token_IID)

    return result

  def page7(self):
    """GET hpm (requests 701-702)."""
    self.token_IID = \
      'SERP.1000'
    result = request701.GET('/hpm' +
      '?IID=' +
      self.token_IID +
      '&IG=' +
      self.token_IG)

    request702.GET('/rms/rms answers Notifications close-hvr/ic/a5eb578c/409a194b.png')

    return result

  def page8(self):
    """GET th (request 801)."""
    self.token_id = \
      'OPN.RTNews_zMtpLYxizSnfSL2h-CEeag'
    self.token_w = \
      '150'
    self.token_h = \
      '75'
    self.token_c = \
      '7'
    self.token_rs = \
      '2'
    self.token_qlt = \
      '80'
    self.token_cdv = \
      '1'
    self.token_pid = \
      'News'
    result = request801.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page9(self):
    """GET th (request 901)."""
    self.token_id = \
      'OPN.RTNews_5KHOQFkiqNWHq2-6VHsrEA'
    result = request901.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page10(self):
    """GET th (request 1001)."""
    self.token_id = \
      'OPN.RTNews_flbdnkEetaoBqiHivJqYDw'
    result = request1001.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page11(self):
    """GET th (request 1101)."""
    self.token_id = \
      'OPN.RTNews_OCyx4xpUhFLLEloN30GFqw'
    result = request1101.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page12(self):
    """GET l (request 1201)."""
    self.token_Type = \
      'Event.PPT'
    self.token_DATA = \
      '{\"S\":12,\"E\":481,\"T\":0,\"I\":0,\"N\":{\"H\":{\"S\":239,\"E\":458,\"T\":1}},\"M\":{}}'
    result = request1201.GET('/fd/ls/l' +
      '?IG=' +
      self.token_IG +
      '&CID=' +
      self.token_CID +
      '&Type=' +
      self.token_Type +
      '&DATA=' +
      self.token_DATA +
      '&P=' +
      self.token_P +
      '&DA=' +
      self.token_DA)

    return result

  def page13(self):
    """GET th (request 1301)."""
    self.token_id = \
      'OPN.RTNews_2-cRb5b4mecMpnyNABu_Gw'
    result = request1301.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page14(self):
    """GET th (request 1401)."""
    self.token_id = \
      'OPN.RTNews_zG2IFUbdZZmgmxlOlH3_8g'
    result = request1401.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page15(self):
    """GET th (request 1501)."""
    self.token_id = \
      'OPN.RTNews_bNXRg3OTFOCVCLaGAk3ujw'
    result = request1501.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page16(self):
    """GET th (request 1601)."""
    self.token_id = \
      'OPN.RTNews_qJg3qqbJo8Oh7H1qYfMupA'
    result = request1601.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page17(self):
    """GET th (request 1701)."""
    self.token_id = \
      'OPN.RTNews_xl3ko2oxZN_sSTMs97O47Q'
    result = request1701.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page18(self):
    """GET th (request 1801)."""
    self.token_id = \
      'OPN.RTNews_r3PimIR-SiJqOw7GXcSIUA'
    result = request1801.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page19(self):
    """GET th (request 1901)."""
    self.token_id = \
      'OPN.RTNews_1qxMXaC_iUUMYPWyTPh7xg'
    result = request1901.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page20(self):
    """GET th (request 2001)."""
    self.token_id = \
      'OPN.RTNews_IkS2BJl2q5AdjgkBwwbhWg'
    result = request2001.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page21(self):
    """GET th (request 2101)."""
    self.token_id = \
      'OPN.RTNews_-xpKpD09_xZwGzKYD2Xf_g'
    result = request2101.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page22(self):
    """GET th (request 2201)."""
    self.token_id = \
      'OPN.RTNews_HARTiyHY-s1NhQ32Ed0UAg'
    result = request2201.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page23(self):
    """GET th (request 2301)."""
    self.token_id = \
      'OPN.RTNews_nru_UWU4JebcAqjmuNtmFA'
    result = request2301.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page24(self):
    """GET th (requests 2401-2402)."""
    self.token_id = \
      'OPN.RTNews_NgKa9uJ91o6-uGTitVA9NA'
    result = request2401.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    request2402.GET('/az/hprichbg/rb/RedGrouseScotland_FR-FR13362947184_1920x1080.jpg')

    return result

  def page25(self):
    """POST lsp.aspx (request 2501)."""
    result = request2501.POST('/fd/ls/lsp.aspx',
      '<ClientInstRequest><Events><E><T>Event.ClientInst</T><IG>23F1BE72181943578303D954BED4EBDB</IG><TS>1480665315926</TS><D><![CDATA[[{\"T\":\"CI.BoxModel\",\"FID\":\"CI\",\"Name\":\"v2.8\",\"SV\":\"3\",\"P\":{\"C\":17,\"N\":1,\"I\":\"4xx\",\"S\":\"T+MT\",\"M\":\"V+L+M+MT+E+N+C+K+BD\",\"T\":1450,\"K\":\"SERP,5024.1+11y+SERP,5048.1+SERP,5059.1+SERP.1000,5021.2+IMG+144+146+www.bing.com+http+object+Identity%2520Blue%24BlueIdentityDropdownBootStrap+script+Identity%2520Blue%24BlueIdentityHeader+Identity%2520SnrWindowsLiveConnectBootstrap+Framework+img+xmlhttprequest\",\"F\":0},\"V\":\"6m/0/0/we/ji/we/ji/1/d/visible/default+6z//////j1////\",\"L\":\"70/0/LI#scpt0/@0/v/a/1r/j/9/T+70/1/LI#scpt1/SERP,5025.1/2m/a/26/j/9/T+70/2/LI#scpt2/SERP,5026.1/4s/a/24/j/9/T+70/3/LI#scpt3/SERP,5027.1/6w/a/21/j/9/T+70/4/LI#scpt4/SERP,5028.1/8x/a/2j/j/9/T+70/5/LI#scpt5/SERP,5029.1/bg/a/2d/j/9/T+70/6/LI#hdr_spl//e8/a/3/j/9/T+70/7/LI#msn/SERP,5013.1/eq/a/1t/j/9/T+70/8/LI#office/SERP,5014.1/gj/a/33/j/9/T+70/9/LI#outlook/SERP,5015.1/jm/a/32/j/9/T+70/a/DIV#id_h/@3/0/0/0/0/9/T+70/b/DIV#sw_tfbb//0/0/0/0/9/T+70/c/DIV#sbox/SERP,5095.1/31/3t/sk/19/6/T+70/d/DIV#hp_sw_hdr/@0/0/0/@1/17/6/T+70/e/DIV#hp_bottomCell/@2/0/gq/@1/2b/6/T+70/f/DIV.hp_sw_logo hpcLogoWhite//31/3m/3o/1g/7/T+70/g/DIV.b_searchboxForm//7a/3t/f6/19/9/T+70/h/DIV#sb_foot/SERP,5075.1/0/i2/@1/z/7/T+70/i/DIV#sh_rdiv/@2/x8/gq/3m/14/8/T+70/j/IMG#id_p/@3/0/0/0/0/b/T+dx/k/DIV.sa_as//79/50/f4/0/b/MT+g0/l/DIV#ajaxStyles//0/0/we/0/1/MT+hw/m/IMG.rms_img//f/1i/0/0/8/T+hw/n/IMG.hpn_top_close rms_img//119/1j/a/a/8/T+hw/o/DIV#thp_notf_div/SERP.2000,5005.1/0/17/@1/y/6/MT+j2/p/DIV#carouselControls/SERP.1000,5004.1/16/gq/15/14/8/MT+j2/q/UL#crs_pane/@4/14/i2/29a/31/d/T+j2/r/@5/@4/16/iw/46/23/h/T+j2/s/@5/@4/5g/iw/46/23/h/T+j2/t/@5/@4/9q/iw/46/23/h/T+j2/u/@5/@4/e0/iw/46/23/h/T+j2/v/@5/@4/ia/iw/46/23/h/T+j2/w/@5/@4/mk/iw/46/23/h/T+j2/x/@5/@4/qu/iw/46/23/h/T+j2/y/@5/@4/v4/iw/46/23/h/T+j2/z/@5/@4/ze/iw/46/23/h/T+j2/10/@5/@4/13o/iw/46/23/h/T+j2/11/@5/@4/17y/iw/46/23/h/T+j2/12/@5/@4/1c8/iw/46/23/h/T+j2/13/@5/@4/1gi/iw/46/23/h/T+j2/14/@5/@4/1ks/iw/46/23/h/T+j2/15/@5/@4/1p2/iw/46/23/h/T+j2/16/@5/@4/1tc/iw/46/23/h/T+j2/17/@5/@4/1xm/iw/46/23/h/T+j2/18/@5/@4/21w/iw/46/23/h/T+j2/19/@5/@4/266/iw/46/23/h/T+j2/1a/DIV#sc_mdc/SERP.1000,5021.1/0/i2/@1/33/9/MT+j2/1b/A#sc_imagecreditslink//i/i2/2v/w/8/MT+@6/a///w7//5r/17//R+@6/e////dn//5e//R+@6/i////dn////R+@6/m/////d/d//R+@6/p////dn////R+@6/q////ez////R+@6/r////ft////R+@6/s////ft////R+@6/t////ft////R+@6/u////ft////R+@6/v////ft////R+@6/w////ft////R+@6/x////ft////R+@6/y////ft////R+@6/z////ft////R+@6/10////ft////R+@6/11////ft////R+@6/12////ft////R+@6/13////ft////R+@6/14////ft////R+@6/15////ft////R+@6/16////ft////R+@6/17////ft////R+@6/18////ft////R+@6/19////ft////R+@6/1a////ez////R\",\"E\":\"@7/10/S/b8/http%3A%2F%2Fwww.bing.com%2Fth?id=OPN.RTNews_cAYcS11MsoUj2aWEKvXSow&w=150&h=75&c=7&rs=2&qlt=80&cdv=1&pid=News+@7/11/S/b8/http%3A%2F%2Fwww.bing.com%2Fth?id=OPN.RTNews_HzQ9V5LUkVb8AaNdIZK5zQ&w=150&h=75&c=7&rs=2&qlt=80&cdv=1&pid=News+@7/17/S/b8/http%3A%2F%2Fwww.bing.com%2Fth?id=OPN.RTNews_wIyqWespRNol2ZzRNvuJYg&w=150&h=75&c=7&rs=2&qlt=80&cdv=1&pid=News\",\"N\":\"6r/0//@8/a%2Fhpc18.png/css/j/@9/6r/0/6r/6r/7a/7a+80/1//@8/cpt/@g/1b/@9/80/0/80/80/9b/9b+8x/2//@8/ls%2Flsp.aspx/other/m/@9/8x/0/8x/8x/9k/9k+92/3//@8/BingCore.Bundle/@c/z/@9/92/0/92/93/a2/a2+a8/4//@8/@b/@a/n/@9/a8/0/a8/a8/aw/aw+a8/5//@8/@d/@a/j/@9/a8/0/a8/a8/as/as+a9/6//@8/@e/@a/n/@9/a9/0/a9/a9/aw/aw+a9/7//@8/@f/@a/1d/@9/a9/0/a9/a9/bm/bm+bx/8//@8/@b/@c/7/@9/bx/0/bx/bx/bx/c5+c5/9//@8/@d/@c/g/@9/c5/0/c5/c5/c5/cm+cn/a//@8/@e/@c/2/@9/cn/0/cn/cn/cn/cq+cr/b//@8/@f/@c/4/@9/cr/0/cr/cr/cs/cw+cz/c//@8/rb%2FResurrectionBay_FR-FR9938760197_1920x1080.jpg/@g/1w/@9/cz/0/cz/cz/dx/ew+d2/d//@8/AutoSug/@c/n/@9/d2/0/d2/d3/dq/dq+dc/e//@8/8_1_2_5207728%2FHPImgVidViewer_c.js/@c/h/@9/dc/0/dc/dc/du/du+ey/f//@8/notifications%2Frender/@h/2q/@9/ey/0/ey/f0/ho/ho+ez/g//@8/HPImageArchive.aspx/@h/1m/@9/ez/0/ez/f0/gm/gm+f6/h//@8/8_1_2_5207728%2FHpbCarouselHeaderPopup.js/@c/k/@9/f6/0/f6/f6/fr/fr+fz/i//@8/hpm/@h/2o/@9/fz/0/fz/g0/io/io+hp/j/n/@8/Notifications%2520close-hvr/@g/i/@9/hp/0/hp/hs/i8/i8+iq/k/r/@8/th/@g/w/@9/iq/0/iq/is/jm/jm+iq/l/s/@8/th/@g/13/@9/iq/0/iq/is/ju/ju+ir/m/t/@8/th/@g/r/@9/ir/0/ir/is/ji/jj+ir/n/u/@8/th/@g/v/@9/ir/0/ir/is/jm/jm+ir/o/v/@8/th/@g/1v/@9/ir/0/ir/it/kn/kn+is/p/w/@8/th/@g/1v/@9/it/0/it/it/kn/kn+is/q/x/@8/th/@g/1v/@9/is/0/is/it/kn/kn+is/r/y/@8/th/@g/1q/@9/iu/0/iu/iu/kj/kj+is/s/z/@8/th/@g/1q/@9/is/0/is/iu/kj/kj+it/t/12/@8/th/@g/21/@9/it/0/it/k8/kv/kv+it/u/13/@8/th/@g/28/@9/it/0/it/kj/l2/l2+iu/v/14/@8/th/@g/2p/@9/iu/0/iu/kv/lj/lj+iu/w/15/@8/th/@g/2t/@9/iu/0/iu/kv/ln/ln+iu/x/16/@8/th/@g/2t/@9/iu/0/iu/kv/ln/ln+iv/y/18/@8/th/@g/2s/@9/iv/0/iv/kw/ln/ln+iv/z/19/@8/th/@g/2z/@9/iv/0/iv/l8/lv/lv+j2/10//@8/rb%2FRedGrouseScotland_FR-FR13362947184_1920x1080.jpg/@g/4r/@9/j2/0/j2/ln/mc/nt+jp/11//@8/ppt/@g/p/@9/jp/0/jp/ju/kf/kf\"}]]]></D></E></Events><STS>1480665315926</STS></ClientInstRequest>',
      ( NVPair('Content-Type', 'text/xml'), ))

    return result

  def page26(self):
    """GET l (request 2601)."""
    self.token_TYPE = \
      'Event.ClientInst'
    self.token_DATA = \
      '[{\"T\":\"CI.Init\",\"FID\":\"CI\",\"Name\":\"Base\",\"TS\":1480665314938},{\"Time\":271,\"T\":\"CI.Latency\",\"FID\":\"HP\",\"Name\":\"ImageStart\",\"TS\":1480665314938},{\"Time\":70,\"T\":\"CI.Latency\",\"FID\":\"HP\",\"Name\":\"Image\",\"TS\":1480665315008},{\"Time\":379,\"T\":\"CI.Latency\",\"FID\":\"HP\",\"Name\":\"CarouselStart\",\"TS\":1480665315046},{\"Time\":109,\"T\":\"CI.Latency\",\"FID\":\"HP\",\"Name\":\"Carousel\",\"TS\":1480665315156}]'
    result = request2601.GET('/fd/ls/l' +
      '?IG=' +
      self.token_IG +
      '&CID=' +
      self.token_CID +
      '&TYPE=' +
      self.token_TYPE +
      '&DATA=' +
      self.token_DATA)

    return result

  def page27(self):
    """GET th (request 2701)."""
    self.token_id = \
      'OPN.RTNews_HzQ9V5LUkVb8AaNdIZK5zQ'
    result = request2701.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page28(self):
    """GET th (request 2801)."""
    self.token_id = \
      'OPN.RTNews_cAYcS11MsoUj2aWEKvXSow'
    result = request2801.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page29(self):
    """GET th (request 2901)."""
    self.token_id = \
      'OPN.RTNews_wIyqWespRNol2ZzRNvuJYg'
    result = request2901.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page30(self):
    """GET Passport.aspx (request 3001)."""
    self.token_popup = \
      '1'
    result = request3001.GET('/Passport.aspx' +
      '?popup=' +
      self.token_popup)

    return result

  def page31(self):
    """GET Suggestions (request 3101)."""
    self.token_pt = \
      'page.home'
    self.token_mkt = \
      'fr-fr'
    self.token_qry = \
      ''
    self.token_cp = \
      '0'
    self.token_o = \
      'hs'
    self.token_css = \
      '1'
    self.token_cvid = \
      '23F1BE72181943578303D954BED4EBDB'
    result = request3101.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&css=' +
      self.token_css +
      '&cvid=' +
      self.token_cvid)

    return result

  def page32(self):
    """GET Suggestions (request 3201)."""
    self.token_qry = \
      't'
    result = request3201.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page33(self):
    """GET Suggestions (request 3301)."""
    self.token_qry = \
      'to'
    result = request3301.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page34(self):
    """GET Suggestions (request 3401)."""
    self.token_qry = \
      'tot'
    result = request3401.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page35(self):
    """GET Suggestions (request 3501)."""
    self.token_qry = \
      'toto'
    result = request3501.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page36(self):
    """GET th (request 3601)."""
    self.token_id = \
      'A052a3b36678a5cc281322facad802c36'
    self.token_w = \
      '80'
    self.token_h = \
      '80'
    self.token_c = \
      '6'
    self.token_rs = \
      '1'
    self.token_qlt = \
      '90'
    self.token_p = \
      '0'
    self.token_pid = \
      'RS'
    result = request3601.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&p=' +
      self.token_p +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page37(self):
    """GET Suggestions (request 3701)."""
    self.token_qry = \
      'totol'
    result = request3701.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page38(self):
    """GET Suggestions (request 3801)."""
    self.token_qry = \
      'totola'
    result = request3801.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page39(self):
    """GET th (request 3901)."""
    self.token_id = \
      'Adba425a4431918391618a0da9b5d2793'
    self.token_w = \
      '107'
    self.token_c = \
      '7'
    result = request3901.GET('/th' +
      '?id=' +
      self.token_id +
      '&w=' +
      self.token_w +
      '&h=' +
      self.token_h +
      '&c=' +
      self.token_c +
      '&rs=' +
      self.token_rs +
      '&qlt=' +
      self.token_qlt +
      '&p=' +
      self.token_p +
      '&cdv=' +
      self.token_cdv +
      '&pid=' +
      self.token_pid)

    return result

  def page40(self):
    """GET Suggestions (request 4001)."""
    self.token_qry = \
      'totolas'
    result = request4001.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page41(self):
    """GET Suggestions (request 4101)."""
    self.token_qry = \
      'totolast'
    result = request4101.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page42(self):
    """GET Suggestions (request 4201)."""
    self.token_qry = \
      'totolasti'
    result = request4201.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page43(self):
    """GET Suggestions (request 4301)."""
    self.token_qry = \
      'totolastic'
    result = request4301.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page44(self):
    """GET Suggestions (request 4401)."""
    self.token_qry = \
      'totolastico'
    result = request4401.GET('/AS/Suggestions' +
      '?pt=' +
      self.token_pt +
      '&mkt=' +
      self.token_mkt +
      '&qry=' +
      self.token_qry +
      '&cp=' +
      self.token_cp +
      '&o=' +
      self.token_o +
      '&cvid=' +
      self.token_cvid)

    return result

  def page45(self):
    """POST lsp.aspx (request 4501)."""
    result = request4501.POST('/fd/ls/lsp.aspx',
      '<ClientInstRequest><Events><E><T>Event.ClientInst</T><IG>23F1BE72181943578303D954BED4EBDB</IG><TS>1480665337675</TS><D><![CDATA[[{\"T\":\"CI.BoxModel\",\"FID\":\"CI\",\"Name\":\"v2.8\",\"SV\":\"3\",\"P\":{\"C\":9,\"N\":2,\"I\":\"4xx\",\"S\":\"C+BD+MT+K\",\"M\":\"V+L+M+MT+E+N+C+K+BD\",\"T\":23201,\"K\":\"hwc+14g+153+9rj+9sv+fhv+fhw+AS%2FSuggestions+g34+g5y+g9x+g8r+gbs+gbk+gec+gey+ghc+gk4+gpu+gsi+gsj+gu5+gsq+gv9+gyd+h0p+h16+h3e+h6s+h8y+h9l+hbp+mousemove+fvz+keydown+g1i+g4e+keypress+g7y+gam+goe+gr2+gxq+h0e+h7i+hbi+es4\",\"F\":0},\"L\":\"@o/1c/DIV#focus_ovr//0/0/@1/i2/6/MT+@1f/1d/INPUT#sb_form_q//7j/3z/dn/u/a/K+g5q/1e/UL#sa_ul//79/50/f4/6w/c/MT+@i/k///0/0/0///R+@i/1e///0/0/0/0/0/R\",\"N\":\"iv/12/19/@8/th/@g/2z/@9/iv/0/iv/l8/lv/lv+j2/13//@8/rb%2FRedGrouseScotland_FR-FR13362947184_1920x1080.jpg/@g/4r/@9/j2/0/j2/ln/mc/nt+jp/14//@8/ppt/@g/p/@9/jp/0/jp/ju/kf/kf+@j/15//@8/ls%2Flsp.aspx/@h/m/@9/@j/0/@j/@j/@k/@k+1it/16//login.live.com/login.srf/iframe/8a1/https/@l/0/@l/9rm/@m/@m+1wk/17//a4.bing.com/clientinst/@g/2w/@9/-1/0/-1/-1/-1/1zg+@n/18//@8/@p/@h/24/@9/@n/0/@n/@o/fjz/fk0+@q/19//@8/@p/@h/2j/@9/@q/0/@q/@q/g5n/g5o+@r/1a//@8/@p/@h/3z/@9/@r/0/@r/@r/@s/@s+@t/1b//@8/@p/@h/31/@9/@t/0/@t/@t/@u/@u+@v/1c//@8/@p/@h/2q/@9/@v/0/@v/gbl/gea/geb+@w/1d//@8/th/@g/l/@9/@w/0/@w/gee/@x/@x+@y/1e//@8/@p/@h/2r/@9/@y/0/@y/ghd/@z/@z+@10/1f//@8/@p/@h/2o/@9/@10/0/@10/@10/@11/@11+@12/1g//@8/th/@g/1m/@9/@12/0/@12/gsm/@13/@13+@14/1h//@8/@p/@h/2i/@9/@14/0/@14/gsr/@15/@15+@16/1i//@8/@p/@h/2c/@9/@16/0/@16/@16/@17/@17+@18/1j//@8/@p/@h/28/@9/@18/0/@18/@18/@19/@19+@1a/1k//@8/@p/@h/26/@9/@1a/0/@1a/@1a/@1b/@1b+@1c/1l//@8/@p/@h/24/@9/@1c/0/@1c/@1c/@1d/@1d\",\"C\":\"@1s/o/@1e/mouse/0//1w/0+esu/////1d/1m/+etl/////2q/19/+euu/1////42/t/+exb/2////5g/m/+eyi/o////6r/1c/+ezf/////7y/24/+f0h/////92/2z/+fg6/1d/mousedown///9q/42/1+fhu//mouseup/////0+fn1//@1e///9v/40/\",\"K\":\"@1f/1d/@1g/1eki+@1h/1d/@1g/2+@1h/1d/@1j/2+@1i/1d/@1g/2+@1i/1d/@1j/2+@1k/1d/@1g/2+@1k/1d/@1j/2+@1l/1d/@1g/2+@1l/1d/@1j/2+gfq/1d/@1g/2+gfr/1d/@1j/2+@1m/1d/@1g/2+@1m/1d/@1j/2+@1n/1d/@1g/2+@1n/1d/@1j/2+@1o/1d/@1g/2+@1o/1d/@1j/2+@1p/1d/@1g/2+@1p/1d/@1j/2+h4e/1d/@1g/2+h4f/1d/@1j/2+@1q/1d/@1g/2+@1q/1d/@1j/2+@1r/1d/@1g/2+@1r/1d/@1j/2\",\"BD\":\"@1s/@1e/1480665334\"}]]]></D></E></Events><STS>1480665337675</STS></ClientInstRequest>',
      ( NVPair('Content-Type', 'text/xml'), ))

    return result

  def page46(self):
    """POST lsp.aspx (request 4601)."""
    result = request4601.POST('/fd/ls/lsp.aspx',
      '<ClientInstRequest><Events><E><T>Event.ClientInst</T><IG>23F1BE72181943578303D954BED4EBDB</IG><TS>1480665340846</TS><D><![CDATA[[{\"T\":\"CI.BoxModel\",\"FID\":\"CI\",\"Name\":\"v2.8\",\"SV\":\"3\",\"P\":{\"C\":4,\"N\":3,\"I\":\"4xx\",\"S\":\"C\",\"M\":\"V+L+M+MT+E+N+C+K+BD\",\"T\":26374,\"K\":\"hwl+hx7\",\"F\":0},\"N\":\"@1t/1m//@8/ls%2Flsp.aspx/@h/l/@9/@1t/0/@1t/@1t/@1u/@1u\",\"C\":\"jpr/////9u//+jqm/1c////8g/3l/+jqw/////70/3c/+jr4/////5h/34/+jrb/////41/30/+jri/////2e/2y/+jro/////10/2w/+jrq/////6/2u/\"}]]]></D></E></Events><STS>1480665340846</STS></ClientInstRequest>',
      ( NVPair('Content-Type', 'text/xml'), ))

    return result

  def page47(self):
    """GET search (requests 4701-4702)."""
    self.token_q = \
      'TOTOLASTICOT'
    self.token_qs = \
      'n'
    self.token_form = \
      'QBLH'
    self.token_pq = \
      'totolastico'
    self.token_sc = \
      '0-11'
    self.token_sp = \
      '-1'
    self.token_sk = \
      ''
    result = request4701.GET('/search' +
      '?q=' +
      self.token_q +
      '&qs=' +
      self.token_qs +
      '&form=' +
      self.token_form +
      '&pq=' +
      self.token_pq +
      '&sc=' +
      self.token_sc +
      '&sp=' +
      self.token_sp +
      '&sk=' +
      self.token_sk +
      '&cvid=' +
      self.token_cvid)

    grinder.sleep(14)
    request4702.GET('/sa/simg/sw_nh_smallid_hamleft_accstar.png')

    return result

  def page48(self):
    """GET l (request 4801)."""
    self.token_IG = \
      '3B47C9436E854FA6B0FC5B70E58F125E'
    self.token_Type = \
      'Event.CPT'
    self.token_DATA = \
      '{\"pp\":{\"S\":\"L\",\"FC\":13,\"BC\":257,\"SE\":-1,\"TC\":-1,\"H\":272,\"BP\":292,\"CT\":293,\"IL\":2},\"ad\":[-1,-1,1149,685,1150,1257,0]}'
    result = request4801.GET('/fd/ls/l' +
      '?IG=' +
      self.token_IG +
      '&Type=' +
      self.token_Type +
      '&DATA=' +
      self.token_DATA +
      '&P=' +
      self.token_P +
      '&DA=' +
      self.token_DA)

    return result

  def page49(self):
    """POST lsp.aspx (requests 4901-4905)."""
    result = request4901.POST('/fd/ls/lsp.aspx',
      '<ClientInstRequest><Events><E><T>Event.ClientInst</T><IG>3B47C9436E854FA6B0FC5B70E58F125E</IG><TS>1480665345600</TS><D><![CDATA[{id:4217,P:\"18:21,19:21,2:5,3:5,4:5,5:5,7:5,8:8,9:20,10:20,11:21,12:aa,13:ah,14:ah,15:ai,16:ai,17:ai\",S:\"nav:0\",v:1.1,T:\"CI.Perf\",FID:\"CI\",Name:\"PerfV2\"}]]></D></E></Events><STS>1480665345600</STS></ClientInstRequest>',
      ( NVPair('Content-Type', 'text/plain;charset=UTF-8'), ))

    request4902.GET('/rms/rms answers SegmentFilters Blue$GenericDropDownModernCalendar/jc,nj/00613382/d8562e0f.js')

    request4903.GET('/rms/rms answers WebResult Blue$WebResultToolboxBlue/jc,nj/2ae3e834/f0e4bfe8.js')

    request4904.GET('/rms/rms answers VisualSystem Footer$IPv6TestScript/jc,nj/632a1478/640ee89b.js')

    request4905.GET('/rms/rms answers Web Empty/jc,nj/e2ce94e3/68b0925c.js')

    return result

  def page50(self):
    """POST / (request 5001)."""
    result = request5001.POST('/',
      "\x30\x51\x30\x4F\x30\x4D\x30\x4B\x30\x49\x30\x09\x06\x05\x2B\x0E"
      "\x03\x02\x1A\x05\x00\x04\x14\xCF\x26\xF5\x18\xFA\xC9\x7E\x8F\x8C"
      "\xB3\x42\xE0\x1C\x2F\x6A\x10\x9E\x8E\x5F\x0A\x04\x14\x51\x68\xFF"
      "\x90\xAF\x02\x07\x75\x3C\xCC\xD9\x65\x64\x62\xA2\x12\xB8\x59\x72"
      "\x3B\x02\x10\x0E\xCB\x09\x39\xB2\xB1\x01\x54\xB8\x95\x70\xC7\xB2"
      "\x2B\x7A\x47",
      ( NVPair('Content-Type', 'application/ocsp-request'), ))

    return result

  def page51(self):
    """POST lsp.aspx (request 5101)."""
    result = request5101.POST('/fd/ls/lsp.aspx',
      '<ClientInstRequest><Events><E><T>Event.ClientInst</T><IG>3B47C9436E854FA6B0FC5B70E58F125E</IG><TS>1480665346798</TS><D><![CDATA[[{\"T\":\"CI.BoxModel\",\"FID\":\"CI\",\"Name\":\"v2.8\",\"SV\":\"3\",\"P\":{\"C\":14,\"N\":1,\"I\":\"4m3\",\"S\":\"C+BD+T\",\"M\":\"V+L+M+MT+E+N+C+K+BD\",\"T\":1576,\"K\":\"SPAN.ftrB+LI.b_algo+SERP,5024.1+www.bing.com+img+http+object+Framework+script+Identity%2520Blue%24BlueIdentityDropdownBootStrap+Identity%2520Blue%24BlueIdentityHeader+Identity%2520SnrWindowsLiveConnectBootstrap+SegmentFilters%2520Blue%24GenericDropDownModernCalendar+WebResult%2520Blue%24WebResultToolboxBlue+VisualSystem%2520Footer%24IPv6TestScript+Web%2520Empty+mousemove\",\"F\":0},\"V\":\"2r/0/0/we/ji/vy/3d/1/d/visible/default+9y//////yx////+fl//////zd////\",\"L\":\"2r/0/NAV.b_scopebar/SERP,5016.1/2s/29/t1/14/2/T+2r/1/H1.b_logo//1m/g/s/10/4/T+2r/2/DIV.b_searchboxForm//2s/e/fk/16/3/T+2r/3/DIV#id_h/@2/0/0/0/0/2/T+9y/4/SPAN.sb_count//3c/3q/3p/u/3/T+9y/5/@0/SERP,5273.1/71/3q/2r/u/3/T+9y/6/@0/SERP,5280.1/9s/3q/37/u/3/T+9y/7/@0/SERP,5284.1/cz/3q/23/u/3/T+9y/8/@1/SERP,5121.1/2s/4k/fk/2l/3/T+9y/9/@1/SERP,5132.1/2s/77/fk/2n/3/T+9y/a/@1/SERP,5144.1/2s/9v/fk/2n/3/T+9y/b/@1/SERP,5155.1/2s/ck/fk/26/3/T+9y/c/@1/SERP,5170.1/2s/er/fk/3d/3/T+9y/d/@1/SERP,5182.1/2s/i6/fk/2n/3/T+9y/e/@1/SERP,5195.1/2s/kv/fk/2n/3/T+9y/f/@1/SERP,5210.1/2s/nj/fk/2n/3/T+9y/g/@1/SERP,5222.1/2s/q8/fk/2n/3/T+9y/h/@1/SERP,5233.1/2s/sw/fk/2n/3/T+9y/i/LI.b_pag/SERP,5310.1/2s/vl/fk/2q/3/T+9y/j/FOOTER.b_footer/SERP,5040.1/0/yx/vy/1c/1/T+2r/k/IMG//1m/g/1f/2l/5/T+9y/k/////jb/1q//T+2r/l/IMG#id_p/@2/0/0/0/0/4/T+17p/3///rn/a/46/1e//R\",\"N\":\"2d/0/k/@3/simg%2Fsw_nh_smallid_hamleft_accstar.png/@4/i/@5/2d/0/2d/2e/2w/2w+al/1//@3/cpt/@4/l/@5/al/0/al/al/b7/b7+an/2//@3/ls%2Flsp.aspx/other/n/@5/an/0/an/ao/bb/bb+ao/3//@3/BingCore.Bundle/@8/0/@5/ao/0/ao/ao/ap/ap+az/4//@3/@7/@6/b/@5/az/0/az/az/az/ba+az/5//@3/@9/@6/d/@5/az/0/az/az/az/bd+b0/6//@3/@a/@6/f/@5/b0/0/b0/b0/b0/bf+b0/7//@3/@b/@6/i/@5/b0/0/b0/b0/b0/bi+b0/8//@3/@c/@6/k/@5/b0/0/b0/b0/bk/bk+b0/9//@3/@d/@6/k/@5/b0/0/b0/b1/bl/bl+b1/a//@3/@e/@6/j/@5/b1/0/b1/b1/bl/bl+b1/b//@3/@f/@6/f/@5/b1/0/b1/b1/bh/bh+co/c//@3/@7/@8/y/@5/co/0/co/co/co/dm+dy/d//@3/@9/@8/3/@5/dy/0/dy/dy/dy/e1+e2/e//@3/@a/@8/0/@5/e2/0/e2/e2/e2/e2+e3/f//@3/@b/@8/5/@5/e3/0/e3/e3/e3/e9+e9/g//@3/@c/@8/5/@5/e9/0/e9/e9/ea/ef+ej/h//@3/@d/@8/0/@5/ej/0/ej/ej/ej/ek+en/i//@3/@e/@8/f/@5/en/0/en/en/en/f3+f4/j//@3/@f/@8/d/@5/f4/0/f4/f4/f4/fi\",\"C\":\"fa//@g/mouse/0/m0/4g/0+hc/////kn/4s/+i2/////j9/4y/+in/8////hq/4z/+j2/////ga/53/+jh/////eu/54/+jw/////de/5d/+k9/////c1/5r/+km/////an/64/+ky/////99/6h/+la/////7u/6s/+lm/////6d/72/+lx/9////4w/7h/+m7/////3i/7u/+mi/////22/83/+mu/////8/8b/+mv///////\",\"BD\":\"fa/@g/1480665346\"}]]]></D></E></Events><STS>1480665346798</STS></ClientInstRequest>',
      ( NVPair('Content-Type', 'text/xml'), ))

    return result

  def page52(self):
    """GET trans.gif (request 5201)."""
    result = request5201.GET('/apc/trans.gif')

    return result

  def page53(self):
    """GET trans.gif (request 5301)."""
    result = request5301.GET('/apc/trans.gif')

    return result

  def page54(self):
    """GET trans.gif (request 5401)."""
    result = request5401.GET('/apc/trans.gif')

    return result

  def page55(self):
    """GET 17k.gif (request 5501)."""
    result = request5501.GET('/apc/17k.gif' +
      '?1f4050437360130bc63cd6a0a3ed4d39')

    return result

  def page56(self):
    """GET 17k.gif (request 5601)."""
    result = request5601.GET('/apc/17k.gif' +
      '?3ec0094a75dafe6b204eb76d15bea609')

    return result

  def page57(self):
    """GET 17k.gif (request 5701)."""
    result = request5701.GET('/apc/17k.gif' +
      '?d7f441b6532f6652359d9ca3709aa48f')

    return result

  def page58(self):
    """GET Passport.aspx (request 5801)."""
    result = request5801.GET('/Passport.aspx' +
      '?popup=' +
      self.token_popup)

    return result

  def page59(self):
    """GET test (request 5901)."""
    result = request5901.GET('/ipv6test/test')

    return result

  def page60(self):
    """GET trans.gif (request 6001)."""
    self.token_MonitorID = \
      'AZR'
    self.token_rid = \
      '3B47C9436E854FA6B0FC5B70E58F125E'
    self.token_w3c = \
      'true'
    self.token_prot = \
      'http:'
    self.token_v = \
      '4'
    self.token_DATA = \
      '[{\"MonitorID\":\"CLO\",\"RequestID\":\"1f4050437360130bc63cd6a0a3ed4d39\",\"Result\":79},{\"MonitorID\":\"CLO\",\"RequestID\":\"3ec0094a75dafe6b204eb76d15bea609\",\"Result\":84},{\"MonitorID\":\"CLO\",\"RequestID\":\"d7f441b6532f6652359d9ca3709aa48f\",\"Result\":367}]'
    result = request6001.GET('/trans.gif' +
      '?&MonitorID=' +
      self.token_MonitorID +
      '&rid=' +
      self.token_rid +
      '&w3c=' +
      self.token_w3c +
      '&prot=' +
      self.token_prot +
      '&v=' +
      self.token_v +
      '&DATA=' +
      self.token_DATA)

    return result

  def __call__(self):
    """Called for every run performed by the worker thread."""
    self.page1()      # GET / (request 101)

    grinder.sleep(41)
    self.page2()      # GET / (requests 201-203)

    grinder.sleep(18)
    self.page3()      # GET l (request 301)
    self.page4()      # POST lsp.aspx (requests 401-409)

    grinder.sleep(44)
    self.page5()      # GET HPImageArchive.aspx (requests 501-502)
    self.page6()      # GET render (request 601)
    self.page7()      # GET hpm (requests 701-702)
    self.page8()      # GET th (request 801)
    self.page9()      # GET th (request 901)
    self.page10()     # GET th (request 1001)
    self.page11()     # GET th (request 1101)
    self.page12()     # GET l (request 1201)
    self.page13()     # GET th (request 1301)
    self.page14()     # GET th (request 1401)
    self.page15()     # GET th (request 1501)
    self.page16()     # GET th (request 1601)
    self.page17()     # GET th (request 1701)
    self.page18()     # GET th (request 1801)
    self.page19()     # GET th (request 1901)
    self.page20()     # GET th (request 2001)
    self.page21()     # GET th (request 2101)
    self.page22()     # GET th (request 2201)
    self.page23()     # GET th (request 2301)
    self.page24()     # GET th (requests 2401-2402)

    grinder.sleep(654)
    self.page25()     # POST lsp.aspx (request 2501)

    grinder.sleep(1052)
    self.page26()     # GET l (request 2601)

    grinder.sleep(3296)
    self.page27()     # GET th (request 2701)
    self.page28()     # GET th (request 2801)
    self.page29()     # GET th (request 2901)

    grinder.sleep(6767)
    self.page30()     # GET Passport.aspx (request 3001)
    self.page31()     # GET Suggestions (request 3101)

    grinder.sleep(689)
    self.page32()     # GET Suggestions (request 3201)

    grinder.sleep(11)
    self.page33()     # GET Suggestions (request 3301)
    self.page34()     # GET Suggestions (request 3401)

    grinder.sleep(61)
    self.page35()     # GET Suggestions (request 3501)
    self.page36()     # GET th (request 3601)

    grinder.sleep(88)
    self.page37()     # GET Suggestions (request 3701)

    grinder.sleep(206)
    self.page38()     # GET Suggestions (request 3801)
    self.page39()     # GET th (request 3901)
    self.page40()     # GET Suggestions (request 4001)

    grinder.sleep(112)
    self.page41()     # GET Suggestions (request 4101)

    grinder.sleep(17)
    self.page42()     # GET Suggestions (request 4201)

    grinder.sleep(123)
    self.page43()     # GET Suggestions (request 4301)

    grinder.sleep(23)
    self.page44()     # GET Suggestions (request 4401)

    grinder.sleep(752)
    self.page45()     # POST lsp.aspx (request 4501)

    grinder.sleep(3151)
    self.page46()     # POST lsp.aspx (request 4601)
    self.page47()     # GET search (requests 4701-4702)

    grinder.sleep(278)
    self.page48()     # GET l (request 4801)
    self.page49()     # POST lsp.aspx (requests 4901-4905)

    grinder.sleep(467)
    self.page50()     # POST / (request 5001)

    grinder.sleep(685)
    self.page51()     # POST lsp.aspx (request 5101)

    grinder.sleep(244)
    self.page52()     # GET trans.gif (request 5201)

    grinder.sleep(198)
    self.page53()     # GET trans.gif (request 5301)

    grinder.sleep(197)
    self.page54()     # GET trans.gif (request 5401)
    self.page55()     # GET 17k.gif (request 5501)
    self.page56()     # GET 17k.gif (request 5601)
    self.page57()     # GET 17k.gif (request 5701)
    sample = self.page58()     # GET Passport.aspx (request 5801)

    grinder.sleep(156)
    self.page59()     # GET test (request 5901)
    self.page60()     # GET trans.gif (request 6001)
    return sample

# the TestRunner needs to be invoked explicitely now..
tr = TestRunner()
#output.startMeasure("test")
output = tr.__call__()
#output.stopMeasure()
