/*******************************************************************************
 * Copyright (C) 2021, exense GmbH
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
angular.module('asyncTask', [])
  .factory('AsyncTaskService', function ($http, $timeout, Dialogs) {
    const factory = {};

    factory.pollUrl = function (asyncTaskUrl) {
      $http.get(asyncTaskUrl).then(function ({data}) {
        factory.pollCount++;
        if (typeof factory.onProgress === 'function') {
          factory.onProgress(data.progress, data.status);
        }
        if (data.ready) {
          if (data.warnings !== undefined && data.warnings !== null &&
            data.warnings.length > 0) {
            Dialogs.showListOfMsgs(data.warnings).then(function () {
              factory.finish(data);
            })
          } else {
            factory.finish(data);
          }
        } else {
          $timeout(() => factory.pollUrl(asyncTaskUrl), 500);
        }
      });
    }

    factory.finish = function (data) {
      if (data.result) {
        factory.promise.resolve(data.result);
      } else {
        factory.promise.reject();
      }
    }

    factory.poll = function (taskId, onProgress) {
      factory.pollCount = 0;
      factory.pollUrl('rest/controller/async-task/' + taskId + '');
    }

    /*
     *  public function to POST an asynchronous request
     *
     *  requestUrl:string the full request url
     *  onProgress:function(progress:float, [status:string]) (optional) callback function to get progress updates if available
     */
    factory.post = function (requestUrl, onProgress) {
      return new Promise((resolve, reject) => {
        factory.promise = {resolve, reject};
        $http.post(requestUrl).then(function ({data}) {
          factory.onProgress = onProgress;
          factory.poll(data.id);
        })
      });
    };

    /*
    *  public function to GET an asynchronous request
    *
    *  requestUrl:string the full request url
    *  onProgress:function(progress:float, [status:string]) (optional) callback function to get progress updates if available
    */
    factory.get = function (requestUrl, onProgress) {
      return new Promise((resolve, reject) => {
        factory.promise = {resolve, reject};
        $http.get(requestUrl).then(function ({data}) {
          factory.onProgress = onProgress;
          factory.poll(data.id);
        })
      });
    };

    return factory;
  });
