/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
(function($, window) {

	$.fn.contextMenu = function(settings) {

		return this.each(function() {

			// Open context menu
			$(this).on(
					"contextmenu",
					function(e) {
						// open menu
						$(settings.menuSelector).data("invokedOn", $(e.target))
								.show().css({
									position : "absolute",
									left : getLeftLocation(e),
									top : getTopLocation(e)
								}).off('click').on(
										'click',
										function(e) {
											$(this).hide();

											var $invokedOn = $(this).data(
													"invokedOn");
											var $selectedMenu = $(e.target);

											settings.menuSelected.call(this,
													$invokedOn, $selectedMenu);
										});

						return false;
					});

			// make sure menu closes on any click
			$(document).click(function() {
				$(settings.menuSelector).hide();
			});
		});

		function getLeftLocation(e) {
			var mouseWidth = e.pageX;
			var pageWidth = $(window).width();
			var menuWidth = $(settings.menuSelector).width();

			// opening menu would pass the side of the page
			if (mouseWidth + menuWidth > pageWidth && menuWidth < mouseWidth) {
				return mouseWidth - menuWidth;
			}
			return mouseWidth;
		}

		function getTopLocation(e) {
			var mouseHeight = e.pageY;
			var pageHeight = $(window).height();
			var menuHeight = $(settings.menuSelector).height();

			// opening menu would pass the bottom of the page
			if (mouseHeight + menuHeight > pageHeight
					&& menuHeight < mouseHeight) {
				return mouseHeight - menuHeight;
			}
			return mouseHeight;
		}

	};
})(jQuery, window);
