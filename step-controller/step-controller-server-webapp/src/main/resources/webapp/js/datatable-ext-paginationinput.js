$.extend($.fn.dataTableExt.oStdClasses, {
    'sPageEllipsisInput': 'paginate_ellipsisinput',
    'sPageNumber': 'paginate_number',
    'sPageNumbers': 'paginate_numbers',
    'sInputWrapper': 'paginate_input-wrapper',
    'sPageInput': 'paginate_input',
    'sPageTotal': 'paginate_total',
    'sPagePreviousContainer': 'paginate_previous-container',    
    'sPageNextContainer': 'paginate_next-container'
});
 
$.fn.dataTableExt.oPagination.ellipsesInput = {
    'oDefaults': {
        'iShowPages': 7
    },
    'fnClickHandler': function(e) {
        var fnCallbackDraw = e.data.fnCallbackDraw,
            oSettings = e.data.oSettings,
            sPage = e.data.sPage;
 
        if ($(this).is('[disabled]')) {
            return false;
        }
 
        oSettings.oApi._fnPageChange(oSettings, sPage);
        fnCallbackDraw(oSettings);
 
        return true;
    },
    // fnInit is called once for each instance of pager
    'fnInit': function(oSettings, nPager, fnCallbackDraw) {
        var oClasses = oSettings.oClasses,
            oLang = oSettings.oLanguage.oPaginate,
            that = this;
 
        var iShowPages = oSettings.oInit.iShowPages || this.oDefaults.iShowPages;
         
        $.extend(oSettings, {
            _iShowPages: iShowPages
        });
        
        var oPrevious = $('<li class="paginate_button ' + oClasses.sPagePreviousContainer + '"><a class="' + oClasses.sPageButton + ' ' + oClasses.sPagePrevious + '">' + oLang.sPrevious + '</a></li>'),
            oNumbers = $('<li class="' + oClasses.sPageNumbers + '"></li>'),
            oInput = $('<li class="' + oClasses.sInputWrapper + '"><span><input style="margin-top:-2px; margin-bottom:-2px;width: 30px;height:24px;" type="text" class="' + oClasses.sPageInput + '"></input> <span>of</span> <span class="' + oClasses.sPageTotal + '">' + oSettings._iTotalPages + '</span></span></li>'),
            oNext = $('<li class="paginate_button ' + oClasses.sPageNextContainer + '"><a class="' + oClasses.sPageButton + ' ' + oClasses.sPageNext + '">' + oLang.sNext + '</a></li>'),
            oPaginationList = $('<ul class="pagination"></ul>');
 
        oPrevious.click({ 'fnCallbackDraw': fnCallbackDraw, 'oSettings': oSettings, 'sPage': 'previous' }, that.fnClickHandler);
        oNext.click({ 'fnCallbackDraw': fnCallbackDraw, 'oSettings': oSettings, 'sPage': 'next' }, that.fnClickHandler);

        // Draw
        $(oPaginationList).append(oPrevious, oNumbers, oInput, oNext);
        $(nPager).append(oPaginationList);
        
        $(oInput).find('.' + oClasses.sPageInput).keyup(function (e) {
    
          // 38 = up arrow, 39 = right arrow
          if (e.which === 38 || e.which === 39) {
              this.value++;
          }
          // 37 = left arrow, 40 = down arrow
          else if ((e.which === 37 || e.which === 40) && this.value > 1) {
              this.value--;
          }

          if (this.value === '' || this.value.match(/[^0-9]/)) {
              /* Nothing entered or non-numeric character */
              this.value = this.value.replace(/[^\d]/g, ''); // don't even allow anything but digits
              return;
          }
    
          var iNewStart = oSettings._iDisplayLength * (this.value - 1);
          if (iNewStart < 0) {
              iNewStart = 0;
          }
          if (iNewStart >= oSettings.fnRecordsDisplay()) {
              iNewStart = (Math.ceil((oSettings.fnRecordsDisplay()) / oSettings._iDisplayLength) - 1) * oSettings._iDisplayLength;
          }
  
          oSettings._iDisplayStart = iNewStart;
          oSettings.oInstance.trigger("page.dt", oSettings);
          fnCallbackDraw(oSettings);
      });
  
      // Take the brutal approach to cancelling text selection.
      //$('span', nPager).bind('mousedown', function () { return false; });
      //$('span', nPager).bind('selectstart', function() { return false; });

    },
    // fnUpdate is only called once while table is rendered
    'fnUpdate': function(oSettings, fnCallbackDraw) {
        var oClasses = oSettings.oClasses,
            that = this;
 
        var tableWrapper = oSettings.nTableWrapper;
 
        // Update stateful properties
        this.fnUpdateState(oSettings);
 
        if (oSettings._iCurrentPage === 1) {
            $('.' + oClasses.sPagePrevious, tableWrapper).attr('disabled', true);
            $('.' + oClasses.sPagePreviousContainer, tableWrapper).addClass('disabled');
        } else {
            $('.' + oClasses.sPagePrevious, tableWrapper).removeAttr('disabled');
            $('.' + oClasses.sPagePreviousContainer, tableWrapper).removeClass('disabled');
        }
 
        if (oSettings._iTotalPages === 0 || oSettings._iCurrentPage === oSettings._iTotalPages) {
            $('.' + oClasses.sPageNext, tableWrapper).attr('disabled', true);
            $('.' + oClasses.sPageNextContainer, tableWrapper).addClass('disabled');
        } else {
            $('.' + oClasses.sPageNext, tableWrapper).removeAttr('disabled');
            $('.' + oClasses.sPageNextContainer, tableWrapper).removeClass('disabled');
        }
 
        var i, oNumber, oNumbers = $('.' + oClasses.sPageNumbers, tableWrapper), oInputWrapper = $('.' + oClasses.sInputWrapper, tableWrapper), oInput = $('.' + oClasses.sPageInput, tableWrapper), oPageTotal = $('.' + oClasses.sPageTotal, tableWrapper);
 
        // Erase
        oNumbers.html('');
 
        for (i = oSettings._iFirstPage; i <= oSettings._iLastPage; i++) {
            oNumber = $('<a class="' + oClasses.sPageButton + ' ' + oClasses.sPageNumber + '">' + oSettings.fnFormatNumber(i) + '</a>');
 
            if (oSettings._iCurrentPage === i) {
                oNumber.attr('disabled', true);
                oNumber.addClass('active');
            } else {
                oNumber.click({ 'fnCallbackDraw': fnCallbackDraw, 'oSettings': oSettings, 'sPage': i - 1 }, that.fnClickHandler);
            }
 
            // Draw
            oNumbers.append(oNumber);
        }

        if (oSettings._iShowPages < oSettings._iTotalPages) {
          oNumbers.addClass('hide');
          oInputWrapper.removeClass('hide');
          oPageTotal.text(oSettings._iTotalPages);
          oInput.val(oSettings._iCurrentPage);
        } else  {
          oNumbers.removeClass('hide');
          oInputWrapper.addClass('hide');
        }
    },
    // fnUpdateState used to be part of fnUpdate
    // The reason for moving is so we can access current state info before fnUpdate is called
    'fnUpdateState': function(oSettings) {
        var iCurrentPage = Math.ceil((oSettings._iDisplayStart + 1) / oSettings._iDisplayLength),
            iTotalPages = Math.ceil(oSettings.fnRecordsDisplay() / oSettings._iDisplayLength),
            iFirstPage = 1,
            iLastPage = iTotalPages;
 
        $.extend(oSettings, {
            _iCurrentPage: iCurrentPage,
            _iTotalPages: iTotalPages,
            _iFirstPage: iFirstPage,
            _iLastPage: iLastPage
        });
    }
};