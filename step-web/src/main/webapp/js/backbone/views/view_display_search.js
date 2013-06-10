var SearchDisplayView = Backbone.View.extend({
    el: function () {
        return $(".passageContainer").eq(this.model.get("passageId"));
    },

    /**
     * Initialises - should be called with the options set.
     */
    initialize: function () {
        Backbone.Events.on(this.options.searchType + ":new:" + this.model.get("passageId"), this.render, this);
        this.passageContent = this.$el.find(".passageContent");
    },

    render: function (resultsWrapper) {

        console.log("Rendering search results [parent block]");
        var searchResults = resultsWrapper.searchQueryResults;
        var query = step.util.undoReplaceSpecialChars(searchResults.query);

        this._updateTotal(searchResults.total, resultsWrapper.pageNumber);
        this.lastSearch = searchResults.query;

        var results;
        if (searchResults.total == 0 || searchResults.results.length == 0) {
            results = $("<div>").append(__s.search_no_search_results_found);
        } else if (searchResults.maxReached) {
            this._notApplicableMessage(results, __s.search_too_many_results);
        } else {
            results = this.renderSearch(searchResults, query);

            if (searchResults.strongHighlights) {
                this._highlightStrongs(results, searchResults.strongHighlights);
            } else {
                this._highlightResults(results, query);
            }
        }

        step.util.ui.emptyOffDomAndPopulate(this.passageContent, this._doSpecificSearchRequirements(query, results, resultsWrapper));
    },

    /**
     * An ability to post-process the results at the very end
     * @param query the query syntax
     * @param results the all-emcompassing results object
     * @param resultsWrapper the wrapper containing all meta information about this current search
     * @private
     */
    _doSpecificSearchRequirements: function (query, results, resultsWrapper) {
        //do nothing
        return results;
    },


    _doResultsRender: function (passageId, searchQueryResults, pageNumberArg, highlightTerms, query) {
        this._displayResults(searchQueryResults, passageId);


        this._doFonts(passageId);
        step.util.ui.addStrongHandlers(passageId, step.util.getPassageContainer(passageId));
        this._doSpecificSearchRequirements(passageId, query);
    },


    //TODO this should be done differently now?
    _doFonts: function (passageId) {
        $.each($(".passageContentHolder", step.util.getPassageContainer(passageId)), function (n, item) {
            if (step.util.isUnicode(item)) {
                $(item).addClass("unicodeFont");

                if (step.util.isHebrew(item)) {
                    $(item).addClass("hbFont");
                }
            }
        });
    },

    _updateTotal: function (total, pageNumber) {
        var resultsLabel = step.util.getPassageContainer(this.$el).find("fieldset:visible .resultsLabel");

        //1 = 1 + (pg1 - 1) * 50, 51 = 1 + (pg2 -1) * 50
        var start = total == 0 ? 0 : 1 + ((pageNumber - 1) * (this.options.paged ? step.defaults.pageSize : 1000000));
        var end = pageNumber * step.search.pageSize;
        end = end < total ? end : total;

        resultsLabel.html(sprintf(__s.paging_showing_x_to_y_out_of_z_results, start, end, total));

        this.totalResults = total;
    },

    _highlightResults: function (results, query) {
        var highlightTerms = this._highlightingTerms(query);

        if (highlightTerms == undefined || results == undefined) {
            step.search.highlightTerms = [];
            return;
        }

        for (var i = 0; i < highlightTerms.length; i++) {
            if (!step.util.isBlank(highlightTerms[i])) {
                var regex = new RegExp("\\b" + highlightTerms[i] + "\\b", "ig");
                doHighlight(results.get(0), "secondaryBackground", regex);
            }
        }
    },

    _highlightStrongs: function (results, strongsList) {
        if (strongsList == undefined) {
            return;
        }

        //now let's iterate through the list of strongs, find all the elements that match, and add the highlight class

        for (var i = 0; i < strongsList.length; i++) {
            $("span[strong~='" + strongsList[i] + "']", results).addClass("secondaryBackground");
        }
    },

    _highlightingTerms: function (query) {
        var terms = [];
        var termBase = query.substring(query.indexOf('=') + 1);

        //remove the search in (v1, v2, v3)
        termBase = termBase.replace("#plus#", "")
        termBase = termBase.replace(/in \([^)]+\)/gi, "");
        termBase = termBase.replace("=>", " ")

        //remove range restrictions, -word and -"a phrase"
        termBase = termBase.replace(/[+-]\[[^\]]*]/g, "");
        termBase = termBase.replace(/-[a-zA-Z]+/g, "");
        termBase = termBase.replace(/-"[^"]+"/g, "");

        //remove distances and brackets
        termBase = termBase.replace(/~[0-9]+/g, "");
        termBase = termBase.replace(/[\(\)]*/g, "");
        termBase = termBase.replace(/ AND /g, " ");

        termBase = termBase.replace("+", "");

        var matches = termBase.match(/"[^"]*"/);
        if (matches) {
            for (var i = 0; i < matches.length; i++) {
                terms.push(matches[i].substring(1, matches[i].length - 1));
            }
        }

        //then remove it from the query
        termBase = termBase.replace(/"[^"]*"/, "");
        var smallTerms = termBase.split(" ");
        if (smallTerms) {
            for (var i = 0; i < smallTerms.length; i++) {
                var consideredTerm = smallTerms[i].trim();
                if (consideredTerm.length != "") {
                    terms.push(consideredTerm);
                }
            }
        }
        return terms;
    },

    _notApplicableMessage: function (results, message) {
        var notApplicable = $("<span>").addClass("notApplicable").html(message);
        results.append(notApplicable);
    }
});