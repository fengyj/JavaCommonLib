package me.fengyj.common.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.fengyj.common.scheduler.CronExpressionDescriptor.CronExpressionPart.*;
import static me.fengyj.common.scheduler.CronExpressionDescriptor.DescriptionType.FULL;

/**
 * https://github.com/voidburn/cron-expression-descriptor
 */
public class CronExpressionDescriptor {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    // Functional implementations
    @FunctionalInterface
    private interface GetDescription {
        String getFor(String description);
    }

    // Constants
    private static final Options DEFAULT_OPTIONS     = new Options();
    private static final String  EMPTY_STRING        = "";
    private static final String  LOCALIZATION_BUNDLE = "localization";

    // Patterns
    private final Pattern   specialCharactersSearchPattern       = Pattern.compile("[/\\-,*]");
    private final Pattern   lastDayOffsetPattern                 = Pattern.compile("L-(\\d{1,2})");
    private final Pattern   weekDayNumberMatches                 = Pattern.compile("(\\d{1,2}W)|(W\\d{1,2})");
    private final Pattern   yearPattern                          = Pattern.compile("(\\d{4})");
    private final Pattern   segmentRangesOrMultipleSearchPattern = Pattern.compile("[/\\-,]");
    private final Pattern   segmentAnyOrMultipleSearchPattern    = Pattern.compile("[*,]");
    private final RxReplace stripTrailingChars                   = new RxReplace("[\\,\\s]*$") {
        @Override
        public String replacement() {
            // Strip all matches
            return "";
        }
    };

    // Data
    public enum DescriptionType {
        FULL,
        TIMEOFDAY,
        SECONDS,
        MINUTES,
        HOURS,
        DAYOFWEEK,
        MONTH,
        DAYOFMONTH,
        YEAR
    }

    // State
    private String         expression;
    private String[]       expressionParts;
    private boolean        parsed;
    private Options options;
    private boolean        use24HourTimeFormat;

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region ACCESSORS

    public void setOptions(final Options options) {
        // Sanity checks
        if (options == null) {
            throw new RuntimeException("Options cannot be null");
        }

        this.options = options;
        this.use24HourTimeFormat = options.isUse24HourTimeFormat();
    }

    public Options getOptions() {
        return options;
    }

    public boolean isUse24HourTimeFormat() {
        return use24HourTimeFormat;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region CONSTRUCTORS

    /**
     * Empty constructor
     */
    public CronExpressionDescriptor() {

    }

    /**
     * Constructor (default system locale)
     *
     * @param expression The complete cron expression
     */
    public CronExpressionDescriptor(final String expression) {
        this(expression, DEFAULT_OPTIONS);
    }

    /**
     * Constructor
     *
     * @param expression The cron expression to describe
     * @param options    The options to use when parsing the expression
     */
    public CronExpressionDescriptor(final String expression, final Options options) {
        // Sanity checks
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("The expression to be described cannot be null or empty");
        }

        this.parsed = false;
        this.expression = expression;
        this.options = options;
        this.use24HourTimeFormat = options.isUse24HourTimeFormat();
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region METHODS

    /**
     * Set the expression that this instance will process next time the {@link #getDescription(DescriptionType)} is
     * called. Default options will be used.
     *
     * @param expression The expression to be parsed
     */
    public void setExpression(final String expression) {
        setExpression(expression, DEFAULT_OPTIONS);
    }

    /**
     * Set the expression and options that this instance will process next time the {@link #getDescription(DescriptionType)} is
     * called.
     *
     * @param expression The new expression to describe
     * @param options    The options to use
     */
    public void setExpression(final String expression, final Options options) {
        // Sanity checks
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("The expression to be described cannot be null or empty");
        }

        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null when setting a new expression");
        }

        this.parsed = false;
        this.expression = expression;
        this.options = options;
        this.use24HourTimeFormat = options.isUse24HourTimeFormat();
    }

    /**
     * Get the full description for the currently configured expression and options
     *
     * @return The cron expression description
     */
    public String getDescription() {
        return getDescription(FULL);
    }

    /**
     * Generates a human readable String for the Cron Expression
     *
     * @param type Which part(s) of the expression to describe
     * @return The cron expression description
     */
    public String getDescription(final DescriptionType type) {
        // Sanity checks (required for the empty constructor)
        if (expression == null || expression.isEmpty() || options == null) {
            throw new IllegalArgumentException("The expression to parse and the options to use cannot be null or empty");
        }

        String description;
        try {
            if (!parsed) {
                final CronExpressionParser parser = new CronExpressionParser(expression, options);
                expressionParts = parser.parse();
                parsed = true;
            }

            switch (type) {
                case TIMEOFDAY:
                    description = GetTimeOfDayDescription();
                    break;
                case HOURS:
                    description = GetHoursDescription();
                    break;
                case MINUTES:
                    description = GetMinutesDescription();
                    break;
                case SECONDS:
                    description = GetSecondsDescription();
                    break;
                case DAYOFMONTH:
                    description = GetDayOfMonthDescription();
                    break;
                case MONTH:
                    description = GetMonthDescription();
                    break;
                case DAYOFWEEK:
                    description = GetDayOfWeekDescription();
                    break;
                case YEAR:
                    description = GetYearDescription();
                    break;
                default:
                    description = getFullDescription();
                    break;
            }
        } catch (final Exception e) {
            if (!options.isThrowExceptionOnParseError()) {
                description = e.getMessage();
            } else {
                throw e;
            }
        }

        // Uppercase the first letter
        description = description.substring(0, 1).toUpperCase() + description.substring(1);

        return description;
    }

    /**
     * Generates the FULL description
     *
     * @return FULL description
     */
    protected String getFullDescription() {
        String description;
        try {
            final String timeSegment = GetTimeOfDayDescription();
            final String dayOfMonthDesc = GetDayOfMonthDescription();
            final String monthDesc = GetMonthDescription();
            final String dayOfWeekDesc = GetDayOfWeekDescription();
            final String yearDesc = GetYearDescription();

            description = String.format("%s%s%s%s%s", timeSegment, dayOfMonthDesc, dayOfWeekDesc, monthDesc, yearDesc);
            description = transformVerbosity(description, options.isVerbose());
        } catch (final Exception e) {
            description = "An error occurred when generating the expression description.  Check the cron expression syntax.";
            if (options.isThrowExceptionOnParseError()) {
                throw new RuntimeException(description, e);
            }
        }

        return description;
    }

    /**
     * Generates a description for only the TIMEOFDAY portion of the expression
     *
     * @return The TIMEOFDAY description
     */
    protected String GetTimeOfDayDescription() {
        final String secondsExpression = expressionParts[0];
        final String minuteExpression = expressionParts[1];
        final String hourExpression = expressionParts[2];
        final StringBuilder description = new StringBuilder();

        // Handle special cases first
        if (!specialCharactersSearchPattern.matcher(minuteExpression).find() && !specialCharactersSearchPattern.matcher(hourExpression).find() && !specialCharactersSearchPattern.matcher(secondsExpression).find()) {
            // Specific time of day (i.e. 10 14)
            description.append("At ").append(formatTime(hourExpression, minuteExpression, secondsExpression));
        } else if (secondsExpression.equals("") && minuteExpression.contains("-") && !minuteExpression.contains(",") && !specialCharactersSearchPattern.matcher(hourExpression).find()) {
            // Minute range in single hour (i.e. 0-10 11)
            final String[] minuteParts = minuteExpression.split("-");
            description.append(String.format("Every minute between %s and %s", formatTime(hourExpression, minuteParts[0]), formatTime(hourExpression, minuteParts[1])));
        } else if (secondsExpression.equals("") && hourExpression.contains(",") && !hourExpression.contains("-") && !specialCharactersSearchPattern.matcher(minuteExpression).find()) {
            // Hours list with single minute (o.e. 30 6,14,16)
            final String[] hourParts = hourExpression.split(",");
            description.append("At");
            for (int i = 0; i < hourParts.length; i++) {
                description.append(" ").append(formatTime(hourParts[i], minuteExpression));

                if (i < (hourParts.length - 2)) {
                    description.append(",");
                }

                if (i == hourParts.length - 2) {
                    description.append(" and ");
                }
            }
        } else {
            // Default time description
            final String secondsDescription = GetSecondsDescription();
            final String minutesDescription = GetMinutesDescription();
            final String hoursDescription = GetHoursDescription();

            description.append(secondsDescription);

            if (description.length() > 0 && minutesDescription.length() > 0) {
                description.append(", ");
            }

            description.append(minutesDescription);

            if (description.length() > 0 && hourExpression.length() > 0) {
                description.append(", ");
            }

            description.append(hoursDescription);
        }

        return description.toString();
    }

    /**
     * Generates a description for only the SECONDS portion of the expression
     *
     * @return The SECONDS description
     */
    protected String GetSecondsDescription() {
        return getSegmentDescription(expressionParts[0],
                                     "every second",
                                     desc -> desc,
                                     desc -> String.format("every %s seconds", desc),
                                     desc -> "seconds %s through %s past the minute",
                                     desc -> {
                                         try {
                                             final int i = Integer.parseInt(desc);

                                             if (desc.equals("0")) {
                                                 return "";
                                             } else if (i < 20) {
                                                 return "at %s seconds past the minute";
                                             } else {
                                                 return "at %s seconds past the minute";
                                             }
                                         } catch (NumberFormatException e) {
                                             // Parse failure, original implementation returs the default string anyway
                                             return "at %s seconds past the minute";
                                         }
                                     },
                                     desc -> {
                                         return  ", %s through %s";
                                     });
    }

    /**
     * Generates a description for only the MINUTE portion of the expression
     *
     * @return The MINUTE description
     */
    protected String GetMinutesDescription() {
        final String secondsExpression = expressionParts[0];

        return getSegmentDescription(expressionParts[1],
                                     "every minute",
                                     desc -> desc,
                                     desc -> String.format("every %s minutes", desc),
                                     desc -> "minutes %s through %s past the hour",
                                     desc -> {
                                         try {
                                             int target = Integer.parseInt(desc);
                                             if (desc.equals("0") && secondsExpression.equals("")) {
                                                 return "";
                                             } else if (target < 20) {
                                                 return "at %s minutes past the hour";
                                             } else {
                                                 return "at %s minutes past the hour";
                                             }
                                         } catch (NumberFormatException e) {
                                             return "at %s minutes past the hour";
                                         }
                                     },
                                     desc -> {

                                         return  ", %s through %s";
                                     });
    }

    /**
     * Generates a description for only the HOUR portion of the expression
     *
     * @return The HOUR description
     */
    protected String GetHoursDescription() {
        final String expression = expressionParts[2];

        return getSegmentDescription(expression,
                                     "every hour",
                                     desc -> formatTime(desc, "0"),
                                     desc -> String.format("every %s hours", desc),
                                     desc -> "between %s and %s",
                                     desc -> "at %s",
                                     desc -> {
                                         return  ", %s through %s";
                                     });
    }

    /**
     * Generates a description for only the DAYOFWEEK portion of the expression
     *
     * @return The DAYOFWEEK description
     */
    protected String GetDayOfWeekDescription() {
        String description;
        if (expressionParts[5].equals("*")) {
            // DOW is specified as * so we will not generate a description and defer to DOM part.
            // Otherwise, we could get a contradiction like "on day 1 of the month, every day"
            // or a dupe description like "every day, every day".
            description = "";
        } else {
            description = getSegmentDescription(expressionParts[5],
                                                ", every day",
                                                desc -> {
                                                    // If we're parsing a frequency the single item can be "7", but we won't have a single item description
                                                    if (desc.equals("7")) {
                                                        return "";
                                                    }

                                                    // Drop "Last" identifier (L) if specified
                                                    if (desc.contains("L")) {
                                                        desc = desc.replace("L", "");
                                                    }

                                                    // Drop "day occurrence" identifier (#) if specified. Only retain the week-day's number.
                                                    if (desc.contains("#")) {
                                                        desc = desc.substring(0, desc.indexOf("#"));
                                                    }

                                                    // Retrieve localized day based on the ENUM entry
                                                    final int dayNum = Integer.parseInt(desc);
                                                    return Day.values()[dayNum].getDesc();
                                                },
                                                desc -> String.format(", every %s days of the week", desc),
                                                desc ->  ", %s through %s",
                                                desc -> {
                                                    String format;
                                                    if (desc.contains("#")) {
                                                        final String dayOfWeekOfMonthNumber = desc.substring(desc.indexOf("#") + 1);
                                                        String dayOfWeekOfMonthDescription = null;
                                                        switch (dayOfWeekOfMonthNumber) {
                                                            case "1":
                                                                dayOfWeekOfMonthDescription = "first";
                                                                break;
                                                            case "2":
                                                                dayOfWeekOfMonthDescription = "second";
                                                                break;
                                                            case "3":
                                                                dayOfWeekOfMonthDescription = "third";
                                                                break;
                                                            case "4":
                                                                dayOfWeekOfMonthDescription = "fourth";
                                                                break;
                                                            case "5":
                                                                dayOfWeekOfMonthDescription = "fifth";
                                                                break;
                                                        }


                                                        format = ", on the " + dayOfWeekOfMonthDescription + " %s of the month";
                                                    } else if (desc.contains("L")) {
                                                        format = ", on the last %s of the month";
                                                    } else {
                                                        format = ", only on %s";
                                                    }

                                                    return format;
                                                },
                                                desc ->  ", %s through %s");
        }

        return description;
    }

    /**
     * Generates a description for only the MONTH portion of the expression
     *
     * @return The MONTH description
     */
    protected String GetMonthDescription() {
        return getSegmentDescription(expressionParts[4],
                                     "",
                                     desc -> {
                                         // Retrieve localized month name based on ENUM entry
                                         final int monthNum = Integer.parseInt(desc) - 1; // Offset to match the enum's ordinals
                                         return Month.values()[monthNum].getDesc();
                                     }, desc -> String.format(", every %s months", desc),
                                     desc -> {

                                         return  ", %s through %s";
                                     }, desc -> ", only in %s",
                                     desc -> {

                                         return ", %s through %s";
                                     });
    }

    /**
     * Generates a description for only the DAYOFMONTH portion of the expression
     *
     * @return The DAYOFMONTH description
     */
    protected String GetDayOfMonthDescription() {
        String description;
        final String expression = expressionParts[3];
        switch (expression) {
            case "L":
                description = ", on the last day of the month";
                break;
            case "WL":
            case "LW":
                description = ", on the last weekday of the month";
                break;
            default:
                final Matcher weekDayNumberMatcher = weekDayNumberMatches.matcher(expression);
                if (weekDayNumberMatcher.matches()) {
                    final int weekDayNumber = Integer.parseInt(weekDayNumberMatcher.group(0).replace("W", ""));
                    final String dayString = weekDayNumber == 1 ? "first weekday" : String.format("weekday nearest day %s", weekDayNumber);

                    description = String.format(", on the %s of the month", dayString);
                } else {
                    // Handle "last day offset" (i.e. L-5:  "5 days before the last day of the month")
                    final Matcher lastDayOffsetMatcher = lastDayOffsetPattern.matcher(expression);
                    if (lastDayOffsetMatcher.matches()) {
                        final String offSetDays = lastDayOffsetMatcher.group(1);
                        description = String.format(", %s days before the last day of the month", offSetDays);
                    } else {
                        description = getSegmentDescription(expression,
                                                            ", every day",
                                                            desc -> desc,
                                                            desc -> {
                                                                if (desc.equals("1")) {
                                                                    return ", every day";
                                                                }

                                                                return ", every %s days";
                                                            },
                                                            desc -> ", between day %s and %s of the month",
                                                            desc -> ", on day %s of the month",
                                                            desc -> ", %s through %s");

                    }
                }
                break;
        }

        return description;
    }

    /**
     * Generates a description for only the YEAR portion of the expression
     *
     * @return The YEAR description
     */
    private String GetYearDescription() {
        return getSegmentDescription(expressionParts[6],
                                     "",
                                     desc -> {
                                         if (yearPattern.matcher(desc).matches()) {
                                             final Calendar calendar = Calendar.getInstance();
                                             calendar.set(Integer.parseInt(desc), Calendar.JANUARY, 1);

                                             return String.valueOf(calendar.get(Calendar.YEAR));
                                         }

                                         return desc;
                                     },
                                     desc -> String.format(", every %s years", desc),
                                     desc -> {

                                         return ", %s through %s";
                                     },
                                     desc -> ", only in %s",
                                     desc -> {

                                         return ", %s through %s";
                                     });
    }

    /**
     * Generates the segment description
     * <p>
     * Range expressions used the 'ComaX0ThroughX1' resource
     * However Romanian language has different idioms for
     * 1. 'from number to number' (minutes, seconds, hours, days) -- ComaMinX0ThroughMinX1 optional resource
     * 2. 'from month to month' -- ComaMonthX0ThroughMonthX1 optional resource
     * 3. 'from year to year' -- ComaYearX0ThroughYearX1 optional resource
     * therefore the {@code getRangeFormat} parameter was introduced
     *
     * @param expression                   The expression
     * @param allDescription               The complete description
     * @param getSingleItemDescription     Functional implementation
     * @param getIntervalDescriptionFormat Functional implementation
     * @param getBetweenDescriptionFormat  Functional implementation
     * @param getDescriptionFormat         Functional implementation
     * @param getRangeFormat               Functional implementation that formats range expressions depending on cron parts
     * @return The generated description segment
     */
    protected String getSegmentDescription(final String expression, final String allDescription, final GetDescription getSingleItemDescription, final GetDescription getIntervalDescriptionFormat, final GetDescription getBetweenDescriptionFormat, final GetDescription getDescriptionFormat, final GetDescription getRangeFormat) {
        String description = null;

        if (expression == null || expression.isEmpty()) {
            description = "";
        } else if (expression.equals("*")) {
            description = allDescription;
        } else if (!segmentRangesOrMultipleSearchPattern.matcher(expression).find()) {
            description = String.format(getDescriptionFormat.getFor(expression), getSingleItemDescription.getFor(expression));
        } else if (expression.contains("/")) {
            final String[] segments = expression.split("/");
            description = String.format(getIntervalDescriptionFormat.getFor(segments[1]), getSingleItemDescription.getFor(segments[1]));

            //interval contains 'between' piece (i.e. 2-59/3 )
            if (segments[0].contains("-")) {
                final String betweenSegmentDescription = GenerateBetweenSegmentDescription(segments[0], getBetweenDescriptionFormat, getSingleItemDescription);
                if (!betweenSegmentDescription.startsWith(", ")) {
                    description += ", ";
                }

                description += betweenSegmentDescription;
            } else if (!segmentAnyOrMultipleSearchPattern.matcher(expression).find()) {
                // Strip any leading comma
                final String rangeItemDescription = String.format(getDescriptionFormat.getFor(segments[0]), getSingleItemDescription.getFor(segments[0])).replace(", ", "");

                description += String.format(", starting %s", rangeItemDescription);
            }
        } else if (expression.contains(",")) {
            final String[] segments = expression.split(",");
            final StringBuilder descriptionContent = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i > 0 && segments.length > 2) {
                    descriptionContent.append(",");

                    if (i < segments.length - 1) {
                        descriptionContent.append(" ");
                    }
                }

                if (i > 0 && i == segments.length - 1) {
                    descriptionContent.append(" and ");
                }

                if (segments[i].contains("-")) {
                    String betweenSegmentDescription = GenerateBetweenSegmentDescription(segments[i], getRangeFormat, getSingleItemDescription);

                    //remove any leading comma
                    betweenSegmentDescription = betweenSegmentDescription.replace(", ", "");

                    descriptionContent.append(betweenSegmentDescription);
                } else {
                    descriptionContent.append(getSingleItemDescription.getFor(segments[i]));
                }
            }

            description = String.format(getDescriptionFormat.getFor(expression), descriptionContent);
        } else if (expression.contains("-")) {
            description = GenerateBetweenSegmentDescription(expression, getBetweenDescriptionFormat, getSingleItemDescription);
        }

        return description;
    }

    /**
     * Generates the between segment description
     *
     * @param betweenExpression           Between range expression
     * @param getBetweenDescriptionFormat Functional implementation
     * @param getSingleItemDescription    Functional implementation
     * @return The between segment description
     */
    protected String GenerateBetweenSegmentDescription(final String betweenExpression, final GetDescription getBetweenDescriptionFormat, final GetDescription getSingleItemDescription) {
        final String[] betweenSegments = betweenExpression.split("-");
        final String betweenSegment1Description = getSingleItemDescription.getFor(betweenSegments[0]);
        final String betweenSegment2Description = getSingleItemDescription.getFor(betweenSegments[1]).replace(":00", ":59");
        final String betweenDescriptionFormat = getBetweenDescriptionFormat.getFor(betweenExpression);

        return String.format(betweenDescriptionFormat, betweenSegment1Description, betweenSegment2Description);
    }

    /**
     * Given time parts, will contruct a formatted time description
     *
     * @param hourExpression   Hours part
     * @param minuteExpression Minutes part
     * @return Formatted time description
     */
    protected String formatTime(final String hourExpression, final String minuteExpression) {
        return formatTime(hourExpression, minuteExpression, "");
    }

    /**
     * Given time parts, will contruct a formatted time description
     *
     * @param hourExpression   Hours part
     * @param minuteExpression Minutes part
     * @param secondExpression Seconds part
     * @return Formatted time description
     */
    protected String formatTime(final String hourExpression, final String minuteExpression, final String secondExpression) {
        String period = "";

        int hour = Integer.parseInt(hourExpression);
        if (!use24HourTimeFormat) {
            period = (hour >= 12) ? "PM" : "AM";

            // Prepend leading space
            if (period.length() > 0) {
                period = " " + period;
            }

            // Adjust for 24 hour format
            if (hour == 0) {
                hour = 12;
            }

            if (hour > 12) {
                hour -= 12;
            }
        }

        // Zero pad and assemble time string
        final String hourString = String.format("%02d", hour);
        final String minuteString = String.format("%02d", Integer.parseInt(minuteExpression));
        String secondString = "";
        if (!secondExpression.isEmpty()) {
            secondString = ":" + String.format("%02d", Integer.parseInt(secondExpression));
        }

        return String.format("%s:%s%s%s", hourString, minuteString, secondString, period);
    }

    /**
     * Transforms the verbosity of the expression description by stripping verbosity from original description
     *
     * @param description      The description to transform
     * @param useVerboseFormat If true, will leave description as it, if false, will strip verbose parts.
     *                         The transformed description with proper verbosity
     * @return Formatted description
     */
    protected String transformVerbosity(String description, boolean useVerboseFormat) {
        if (!useVerboseFormat) {
            // Strip minute hour and day if they match their all encompassing statements
            description = description.replace(", every minute", "");
            description = description.replace(", every hour", "");
            description = description.replace(", every day", "");

            // Strip trailing commas and spaces, if any
            description = stripTrailingChars.replace(description);
        }

        return description;
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //region STATIC METHODS


    /**
     * Generates a human readable String for the Cron Expression
     *
     * @param expression The cron expression String
     * @return The requested expression's description
     */
    public static String getDescription(final String expression) {
        return new CronExpressionDescriptor(expression).getDescription(FULL);
    }

    // Data types
    public enum CronExpressionPart {
        SEC("SECOND"),
        MIN("MINUTE"),
        HOUR("HOUR"),
        DOM("DAY OF MONTH"),
        MONTH("MONTH"),
        DOW("DAY OF WEEK"),
        YEAR("YEAR"),
        ALL("EXPRESSION");

        private final String value;

        public String getValue() {
            return value;
        }

        CronExpressionPart(final String value) {
            this.value = value;
        }
    }

    public enum Day {
        SUN("Sunday"),
        MON("Monday"),
        TUE("Tuesday"),
        WED("Wednesday"),
        THU("Thursday"),
        FRI("Friday"),
        SAT("Saturday");

        private String desc;
        private Day(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return this.desc;
        }
    }

    public enum Month {
        JAN("January"),
        FEB("February"),
        MAR("March"),
        APR("April"),
        MAY("May"),
        JUN("June"),
        JUL("July"),
        AUG("August"),
        SEP("September"),
        OCT("October"),
        NOV("November"),
        DEC("December");

        private String desc;
        private Month(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return this.desc;
        }
    }

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public class CronExpressionParser {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region FIELDS

        // Config
        private static final String LOCALIZATION_BUNDLE = "localization";
        private static final int    MIN_YEAR            = 1970;
        private static final int    MAX_YEAR            = 2099;
        private static final int    MIN_YEAR_FREQUENCY  = 0;
        private static final int    MAX_YEAR_FREQUENCY  = MAX_YEAR - MIN_YEAR;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // PLEASE NOTE:
        //
        // Validation patterns are applied to normalized parts, not to the raw expression. Please refer to the
        // normalizeExpression() method for details on how the parts are transformed during that process. This affects the
        // expected patterns we allow for each part.

        // SECONDS and MINUTES in the range and frequencies 0-59
        //
        // ^(?:\\*|^0)$                                         -> Every step {0 or *}
        // ^(?:[0-5]?[0-9])$                                    -> Single value {0-59}
        // ^(?:(?:\\*|[0-5]?[0-9])/[0-5]?[0-9])$                -> Frequency range {* | 0-59}/{0-59} (expressions such as 0/2 are normalized to */2 so must be considered valid)
        // ^(?:([0-5]?[0-9],)*)(?:(?!^)[0-5]?[0-9])$            -> Multiple values {0-59},{0-59},{0-59}...
        // ^(?:[0-5]?[0-9])-(?:[0-5]?[0-9])$                    -> Range {0-59}-{0-59}
        // ^(?:[0-5]?[0-9])-(?:[0-5]?[0-9])/(?:[0-5]?[0-9])$    -> Range AND Frequency {0-59}-{0-59}/{0-59}
        private final Pattern secsAndMinsValidationPattern = Pattern.compile("^(?:\\*|^0)$|^(?:[0-5]?[0-9])$|^(?:(?:\\*|[0-5]?[0-9])/[0-5]?[0-9])$|^(?:([0-5]?[0-9],)*)(?:(?!^)[0-5]?[0-9])$|^(?:[0-5]?[0-9])-(?:[0-5]?[0-9])$|^(?:[0-5]?[0-9])-(?:[0-5]?[0-9])/(?:[0-5]?[0-9])$");

        // HOURS in the range and frequencies 0-23
        //
        // ^(?:\\*|^0)$                                                                                     -> Every step {0 or *}
        // ^(?:[0-1]?[0-9]|2?[0-3])$                                                                        -> Single value {0-23}
        // ^(?:(?:\\*|[0-1]?[0-9])|(?:2[0-3]))/(?:(?:[0-1]?[0-9])|(?:2[0-3]))$                              -> Frequency rage {* | 0-23}/{0-23} (expressions such as 0/2 are normalized to */2 so must be considered valid)
        // ^(?:(?:[0-1]?[0-9],)|(?:2[0-3],))*(?:(?:(?!^)[0-1]?[0-9])|(?:(?!^)2[0-3]))$                      -> Multiple values {0-23},{0-23},{0-23},...
        // ^(?:(?:[0-1]?[0-9])|(?:2[0-3]))-(?:(?:[0-1]?[0-9])|(?:2[0-3]))$                                  -> Range {0-23}-{0-23}
        // ^(?:(?:[0-1]?[0-9])|(?:2[0-3]))-(?:(?:[0-1]?[0-9])|(?:2[0-3]))/(?:(?:[0-1]?[0-9])|(?:2[0-3]))$   -> Range AND Frequency {0-23}-{0-23}/{0-23}
        private final Pattern hoursValidationPattern = Pattern.compile("^(?:\\*|^0)$|^(?:[0-1]?[0-9]|2?[0-3])$|^(?:(?:\\*|[0-1]?[0-9])|(?:2[0-3]))/(?:(?:[0-1]?[0-9])|(?:2[0-3]))$|^(?:(?:[0-1]?[0-9],)|(?:2[0-3],))*(?:(?:(?!^)[0-1]?[0-9])|(?:(?!^)2[0-3]))$|^(?:(?:[0-1]?[0-9])|(?:2[0-3]))-(?:(?:[0-1]?[0-9])|(?:2[0-3]))$|^(?:(?:[0-1]?[0-9])|(?:2[0-3]))-(?:(?:[0-1]?[0-9])|(?:2[0-3]))/(?:(?:[0-1]?[0-9])|(?:2[0-3]))$");

        // DAYS OF MONTH in the range and frequency 1-31
        //
        // ^(?:\\*)$                                                                                                            -> Every step {*}
        // ^(?:[1-9]|1[0-9]|2[0-9]|3[0-1])$                                                                                     -> Single value {1-31}
        // ^(?:\\*|[1-9]|1[0-9]|2[0-9]|3[0-1])/(?:[0-9]|1[0-9]|2[0-9]|3[0-1])$                                                  -> Frequency rage {* | 1-31}/{0-31} (expressions such as 1/31 are normalized to */31 so must be considered valid)
        // ^(?:(?:[1-9],)|(?:1[0-9],)|(?:2[0-9],)|(?:3[0-1],))+(?:(?:[1-9])|(?:1[0-9])|(?:2[0-9])|(?:3[0-1]))$                  -> Multiple values {1-31},{1-31},{1-31},...
        // ^(?:(?:[1-9]|1[0-9]|2[0-9]|3[0-1])-(?:[1-9]|1[0-9]|2[0-9]|3[0-1]))$                                                  -> Range {1-31}-{1-31}
        // ^(?:(?:[1-9]|1[0-9]|2[0-9]|3[0-1])-(?:[1-9]|1[0-9]|2[0-9]|3[0-1]))/(?:[0-9]|1[0-9]|2[0-9]|3[0-1])$                   -> Range AND Frequency {1-31}-{1-31}/{0-31}
        // ^(?:(?:L)|(?:LW)|(?:L)-(?:[1-9]|1[0-9]|2[0-9]|30)|(?:(?:[1-9]|1[0-9]|2[0-9]|3[0-1])W))$                              -> Last day notations {
        //                                                                                                                            L (last day of the month),
        //                                                                                                                            LW (last day of the week),
        //                                                                                                                            L-{1-30} Nth day befor the end of the month,
        //                                                                                                                            {1-31}W On the nearest day to the Nth of the month
        //                                                                                                                         }
        private final Pattern domValidationPattern = Pattern.compile("^(?:\\*)$|^(?:[1-9]|1[0-9]|2[0-9]|3[0-1])$|^(?:\\*|[1-9]|1[0-9]|2[0-9]|3[0-1])/(?:[0-9]|1[0-9]|2[0-9]|3[0-1])$|^(?:(?:[1-9],)|(?:1[0-9],)|(?:2[0-9],)|(?:3[0-1],))+(?:(?:[1-9])|(?:1[0-9])|(?:2[0-9])|(?:3[0-1]))$|^(?:(?:[1-9]|1[0-9]|2[0-9]|3[0-1])-(?:[1-9]|1[0-9]|2[0-9]|3[0-1]))$|^(?:(?:[1-9]|1[0-9]|2[0-9]|3[0-1])-(?:[1-9]|1[0-9]|2[0-9]|3[0-1]))/(?:[0-9]|1[0-9]|2[0-9]|3[0-1])$|^(?:(?:L)|(?:LW)|(?:L)-(?:[1-9]|1[0-9]|2[0-9]|30)|(?:(?:[1-9]|1[0-9]|2[0-9]|3[0-1])W))$");

        // MONTHS in the range and frequencies 1-12
        //
        // ^(?:\*)$                                             -> Every step {*}
        // ^(?:[1-9]|1[0-2])$                                   -> Single value {1-12}
        // ^(?:\\*|[1-9]|1[0-2])/(?:[0-9]|1[0-2])$              -> Frequency range {* | 1-12}/{0-12} (expressions such as 1/12 are normalized to */12 so must be considered valid)
        // ^(?:[1-9],|1[0-2],)*(?:(?!^)[1-9]|(?!^)1[0-2])$      -> Multiple values {1-12},{1-12},{1-12}...
        // ^(?:[1-9]|1[0-2])-(?:[1-9]|1[0-2])$                  -> Range {1-12}-{1-12}
        // ^(?:[1-9]|1[0-2])-(?:[1-9]|1[0-2])/(?:[0-9]|1[0-2])$ -> Range AND Frequency {1-12}-{1-12}/{0-12}
        private final Pattern monthsValidationPattern = Pattern.compile("^(?:\\*)$|^(?:[1-9]|1[0-2])$|^(?:\\*|[1-9]|1[0-2])/(?:[0-9]|1[0-2])$|^(?:[1-9],|1[0-2],)*(?:(?!^)[1-9]|(?!^)1[0-2])$|^(?:[1-9]|1[0-2])-(?:[1-9]|1[0-2])$|^(?:[1-9]|1[0-2])-(?:[1-9]|1[0-2])/(?:[0-9]|1[0-2])$");

        // DAY OF WEEK in the range 0-6
        //
        // ^(?:\*)$                         -> Every step {*}
        // ^(?:[0-6])$                      -> Single value {0-6}
        // ^(?:\\*|[0-6])/(?:[0-6])$        -> Frequency range {* | 0-6}/{0-6} (expressions such as 1/7 are normalized to */7 so must be considered valid)
        // ^(?:[0-6],)*(?:(?!^)[0-6])$      -> Multiple values {0-6},{0-6},{0-6}...
        // ^(?:[0-6])-(?:[0-6])$            -> Range {0-6}-{0-6}
        // ^(?:[0-6])-(?:[0-6])/(?:[0-7])$  -> Range AND Frequency {0-6}-{0-6}/{0-7}
        // ^(?:[0-6]L)$                     -> Last weekday of the month {0-6}L
        // ^(?:[0-6]#[1-5])$                -> Nth Weekday of the month {0-6}#{1-5}
        private final Pattern dowValidationPattern = Pattern.compile("^(?:\\*)$|^(?:[0-6])$|^(?:\\*|[0-6])/(?:[0-6])$|^(?:[0-6],)*(?:(?!^)[0-6])$|^(?:[0-6])-(?:[0-6])$|^(?:[0-6])-(?:[0-6])/(?:[0-7])$|^(?:[0-6]L)$|^(?:[0-6]#[1-5])$");

        // YEARS in the range 1970-2999
        //
        // ^(?:\\*)$                                -> Every step {*}
        // ^\\d{4}$                                 -> Single value {any 4 digit number}
        // ^(?:\\*|\\d{4})/(?:\\d{1,3})$            -> Frequency range {* | any 4 digit number}/{any 3 digit number} (specific validity must be checked outside the match -> 1970-2099 / 1-129)
        // ^(?:\\d{4},)*(?:(?!^)\\d{4})$            -> Multiple values {any 4 digit number},{any 4 digit number},{any 4 digit number}... (specific validity must be checked outside the match -> 1970-2099)
        // ^(?:\\d{4})-(?:\\d{4})$                  -> Range {any 4 digit number}-{any 4 digit number} (specific validity must be checked outside the match -> 1970-2099)
        // ^(?:\\d{4})-(?:\\d{4})/(?:\\d{1,3})$     -> Range AND Frequency {any 4 digit number}-{any 4 digit number}/{any 3 digit number}
        private final Pattern yearsValidationPattern = Pattern.compile("^(?:\\*)$|^\\d{4}$|^(?:\\*|\\d{4})/(?:\\d{1,3})$|^(?:\\d{4},)*(?:(?!^)\\d{4})$|^(?:\\d{4})-(?:\\d{4})$|^(?:\\d{4})-(?:\\d{4})/(?:\\d{1,3})$");

        // Pattern matching
        private final Pattern   yearPattern             = Pattern.compile(".*\\d{4}$");
        private final Pattern   rangeTokenSearchPattern = Pattern.compile("[*/]");
        private final Pattern   stepValueSearchPattern  = Pattern.compile("[*\\-,]");
        private final Pattern   singleItemTokenPattern  = Pattern.compile("^[0-9]+$");
        private final RxReplace dowReplacer             = new RxReplace("(^\\d)|([^#/\\s]\\d)") {
            @Override
            public String replacement() {
                // Skip anything preceeded by # or /
                final String value = group(1) != null ? group(1) : group(2);

                // Extract digit part (i.e. if "-2" or ",2", just take 2)
                final String dowDigits = value.replaceAll("\\D", "");
                String dowDigitsAdjusted = dowDigits;

                // We're about to adjust based on a start index, we should reject out of bounds values before we do so
                if (Integer.parseInt(dowDigits) > 7) {
                    throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "DAY OF WEEK"), DOW);
                }

                // JEE considers 7 and 0 as sunday when specifying DOW (https://docs.oracle.com/javaee/7/tutorial/ejb-basicexamples004.htm)
                if (options.useJavaEeScheduleExpression) {
                    if (dowDigits.equals("7")) {
                        dowDigitsAdjusted = "0";
                    }
                } else {
                    // Adjust Day of Week index for regular cron expressions (5 parts only). In regular cron "7" is accepted as sunday but not considered standard.
                    if (partsCount == 5) {
                        if (dowDigits.equals("7")) {
                            dowDigitsAdjusted = "0";
                        }
                    } else {
                        // If the expression has more than 5 parts (which means it includes seconds and/or years), Sunday is specified as 1 and Saturday is specified as 7.
                        // To normalize, we bring it back in the 0-6 range.
                        //
                        // See Quartz cron triggers (http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)
                        dowDigitsAdjusted = String.valueOf(Integer.parseInt(dowDigits) - 1);
                    }
                }


                return value.replace(dowDigits, dowDigitsAdjusted);
            }
        };

        // State
        private final String         expression;
        private final Options        options;
        private       int            partsCount;

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region SUBCLASSES

        // Parse exception
        public class CronExpressionParseException extends RuntimeException {
            final CronExpressionPart part;

            public CronExpressionPart getPart() {
                return part;
            }

            public CronExpressionParseException(final String message, final CronExpressionPart part) {
                super(message);
                this.part = part;
            }
        }

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region CONSTRUCTORS

        /**
         * Constructor (Init with default options)
         *
         * @param expression The complete cron expression
         */
        public CronExpressionParser(final String expression) {
            this(expression, null);
        }

        /**
         * Constructor
         *
         * @param expression The complete cron expression
         * @param options    Parsing options (null for defaults)
         */
        public CronExpressionParser(final String expression, final Options options) {
            this.expression = expression;
            this.options = options != null ? options : new Options();
        }

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region METHODS

        /**
         * Parses the cron expression string
         *
         * @return A 7 part string array, one part for each component of the cron expression (seconds, minutes, etc.)
         */
        public String[] parse() {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Initialize all elements of parsed array to empty strings
            final String[] parsed = new String[]{"", "", "", "", "", "", ""};
            final String[] tokenizedExpression = expression.split(" ");
            final List<String> tmp = new ArrayList<>();
            for (final String token : tokenizedExpression) {
                if (!token.isEmpty()) {
                    tmp.add(token);
                }
            }

            // Determine how many significant parts the expression contains
            final String[] expressionParts = new String[tmp.size()];
            tmp.toArray(expressionParts);
            partsCount = expressionParts.length;

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Inspect the expression parts
            if (partsCount < 5) {
                throw new CronExpressionParseException(String.format("The cron expression \"%s\" only has [%d] parts. At least 5 parts are required.", expression, partsCount), ALL);
            } else if (partsCount == 5) {
                // 5 part cron so shift array past seconds element
                System.arraycopy(expressionParts, 0, parsed, 1, 5);
            } else if (partsCount == 6) {
                // We will detect if this 6 part expression has a year specified and if so we will shift the parts and treat the
                // first part as a minute part rather than a second part.
                //
                // Ways we detect:
                //   1. Last part is a literal year (i.e. 2020)
                //   2. 3rd or 5th part is specified as "?" (DOM or DOW)
                boolean isYearWithNoSecondsPart = yearPattern.matcher(expressionParts[5]).matches() || expressionParts[4].equals("?") || expressionParts[2].equals("?");
                if (isYearWithNoSecondsPart) {
                    System.arraycopy(expressionParts, 0, parsed, 1, 6);
                } else {
                    System.arraycopy(expressionParts, 0, parsed, 0, 6);
                }
            } else if (partsCount == 7) {
                // All parts are in use
                System.arraycopy(expressionParts, 0, parsed, 0, 7);
            } else {
                if (options.throwExceptionOnParseError) {
                    throw new CronExpressionParseException(String.format("The cron expression \"%s\" has too many parts [%d]. Expressions must not have more than 7 parts.", expression, partsCount), ALL);
                }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Normalize the expression
            normalizeExpression(parsed);

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Validate parts

            // Check if both DoM and DoW have been specified (? is normalized to * at this stage)
            if (partsCount > 5 && (!parsed[3].equals("*") && !parsed[5].equals("*"))) {
                throw new CronExpressionParseException("Specifying both a Day of Month and Day of Week is not supported. Either one or the other should be declared as \"?\"", ALL);
            }

            // Check seconds
            if (partsCount > 5 && (!parsed[0].isEmpty() && !secsAndMinsValidationPattern.matcher(parsed[0]).matches())) {
                throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "SECOND"), SEC);
            }

            // Check minutes
            if (parsed[1].isEmpty() || !secsAndMinsValidationPattern.matcher(parsed[1]).matches()) {
                throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "MINUTE"), MIN);
            }

            // Check hours
            if (parsed[2].isEmpty() || !hoursValidationPattern.matcher(parsed[2]).matches()) {
                throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "HOUR"), HOUR);
            }

            // Check Day of Month
            if (parsed[3].isEmpty() || !domValidationPattern.matcher(parsed[3]).matches()) {
                throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "DAY OF MONTH"), DOM);
            }

            // Check Month
            if (parsed[4].isEmpty() || !monthsValidationPattern.matcher(parsed[4]).matches()) {
                throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "MONTH"), MONTH);
            }

            // Check Day of Week
            if (parsed[5].isEmpty() || !dowValidationPattern.matcher(parsed[5]).matches()) {
                throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "DAY OF WEEK"), DOW);
            }

            // Check year
            if (partsCount > 5 && (!parsed[6].isEmpty() && !yearsValidationPattern.matcher(parsed[6]).matches())) {
                throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "YEAR"), YEAR);
            } else if (!parsed[6].isEmpty() && yearsValidationPattern.matcher(parsed[6]).matches()) {
                if (partsCount > 5 && parsed[6].contains("/")) {
                    final String[] frequencyParts = parsed[6].split("/");
                    if (frequencyParts.length == 2) {
                        // Check range if present
                        if (frequencyParts[0].contains("-")) {
                            final String[] rangeParts = frequencyParts[0].split("-");
                            if (rangeParts.length == 2) {
                                // Check if range parts are out of bounds
                                if (Integer.parseInt(rangeParts[0]) < MIN_YEAR || Integer.parseInt(rangeParts[0]) > MAX_YEAR ||
                                    Integer.parseInt(rangeParts[1]) < MIN_YEAR || Integer.parseInt(rangeParts[1]) > MAX_YEAR) {

                                    throw new CronExpressionParseException(String.format("The expression describing the YEAR field is not in a valid format. Accepted year values are %s-%s", MIN_YEAR, MAX_YEAR), YEAR);
                                }
                            }
                        } else {
                            // Frequency only, validate single year entry
                            if (!frequencyParts[0].equals("*") && (Integer.parseInt(frequencyParts[0]) < MIN_YEAR || Integer.parseInt(frequencyParts[0]) > MAX_YEAR)) {
                                throw new CronExpressionParseException(String.format("The expression describing the YEAR field is not in a valid format. Accepted year values are %s-%s", MIN_YEAR, MAX_YEAR), YEAR);
                            }
                        }

                        // Validate frequency
                        if (Integer.parseInt(frequencyParts[1]) < MIN_YEAR_FREQUENCY || Integer.parseInt(frequencyParts[1]) > MAX_YEAR_FREQUENCY) {
                            throw new CronExpressionParseException(String.format("The expression describing the YEAR field is not in a valid format. Accepted frequency values are %s-%s", MIN_YEAR_FREQUENCY, MAX_YEAR_FREQUENCY), YEAR);
                        }
                    } else {
                        throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", "YEAR"), YEAR);
                    }
                } else if (parsed[6].contains("-")) {
                    // Check if range parts are out of bounds
                    final String[] rangeParts = parsed[6].split("-");
                    if (Integer.parseInt(rangeParts[0]) < MIN_YEAR || Integer.parseInt(rangeParts[0]) > MAX_YEAR ||
                        Integer.parseInt(rangeParts[1]) < MIN_YEAR || Integer.parseInt(rangeParts[1]) > MAX_YEAR) {

                        throw new CronExpressionParseException(String.format("The expression describing the YEAR field is not in a valid format. Accepted year values are %s-%s", MIN_YEAR, MAX_YEAR), YEAR);
                    }
                } else if (!parsed[6].equals("*")) {
                    // Check single value
                    if (Integer.parseInt(parsed[6]) < MIN_YEAR || Integer.parseInt(parsed[6]) > MAX_YEAR) {

                        throw new CronExpressionParseException(String.format("The expression describing the YEAR field is not in a valid format. Accepted year values are %s-%s", MIN_YEAR, MAX_YEAR), YEAR);
                    }
                }
            }

            return parsed;
        }

        /**
         * Massage the parsed expression into a format that can be digested by the ExpressionDescriptor
         *
         * @param parsed The parsed expression parts
         */
        private void normalizeExpression(final String[] parsed) {
            // Convert ? to * only for DOM and DOW
            parsed[3] = parsed[3].replace("?", "*");
            parsed[5] = parsed[5].replace("?", "*");

            // Convert 0/, 1/ to */
            if (parsed[0].startsWith("0/")) {
                // Seconds
                parsed[0] = parsed[0].replace("0/", "*/");
            }

            if (parsed[1].startsWith("0/")) {
                // Minutes
                parsed[1] = parsed[1].replace("0/", "*/");
            }

            if (parsed[2].startsWith("0/")) {
                // Hours
                parsed[2] = parsed[2].replace("0/", "*/");
            }

            if (parsed[3].startsWith("1/")) {
                // DOM
                parsed[3] = parsed[3].replace("1/", "*/");
            }

            if (parsed[4].startsWith("1/")) {
                // Month
                parsed[4] = parsed[4].replace("1/", "*/");
            }

            if (parsed[5].startsWith("1/")) {
                // DOW
                parsed[5] = parsed[5].replace("1/", "*/");
            }

            if (parsed[6].startsWith("1/")) {
                // Years
                parsed[6] = parsed[6].replace("1/", "*/");
            }

            // Adjust DOW based on dayOfWeekStartIndexZero option
            parsed[5] = dowReplacer.replace(parsed[5]);

            // Convert DOM '?' to '*'
            if (parsed[3].equals("?")) {
                parsed[3] = "*";
            }

            // Convert SUN-SAT format to 0-6 format
            for (int i = 0; i <= 6; i++) {
                final String currentDay = Day.values()[i].name();
                parsed[5] = parsed[5].replace(currentDay, String.valueOf(i));

                // Found, early exit
                if (parsed[5].length() == 1) {
                    break;
                }
            }

            // Convert JAN-DEC format to 1-12 format
            for (int i = 0; i < 12; i++) {
                final String currentMonth = Month.values()[i].name();
                parsed[4] = parsed[4].replace(currentMonth, String.valueOf(i + 1));

                // Found, early exit
                if (parsed[4].length() == 1 || parsed[4].length() == 2) {
                    break;
                }
            }

            // Convert 0 second to (empty)
            if (parsed[0].equals("0")) {
                parsed[0] = "";
            }

            // If time interval is specified for seconds or minutes and next time part is single item, make it a "self-range" so
            // the expression can be interpreted as an interval 'between' range.
            //     For example:
            //     0-20/3 9 * * * => 0-20/3 9-9 * * * (9 => 9-9)
            //     */5 3 * * * => */5 3-3 * * * (3 => 3-3)
            if (singleItemTokenPattern.matcher(parsed[2]).matches() && (rangeTokenSearchPattern.matcher(parsed[1]).find() || rangeTokenSearchPattern.matcher(parsed[0]).find())) {
                parsed[2] += "-" + parsed[2];
            }

            // Loop through all parts and apply global normalization
            for (int i = 0; i < parsed.length; i++) {
                // Convert all '*/1' to '*'
                if (parsed[i].equals("*/1")) {
                    parsed[i] = "*";
                }

                // Convert non specified ranges to "/N" -> "*/N"
                final String[] parts = parsed[i].split("/");
                if (parts.length > 1 && parts[0].isEmpty()) {
                    parsed[i] = "*" + "/" + parts[1];
                }

                // Convert Month,DOW,Year step values with a starting value (i.e. not '*') to between expressions.
                // This allows us to reuse the between expression handling for step values.
                //
                // For Example:
                //  - month part '3/2' will be converted to '3-12/2' (every 2 months between March and December)
                //  - DOW part '3/2' will be converted to '3-6/2' (every 2 days between Tuesday and Saturday)
                if (parsed[i].contains("/") && !stepValueSearchPattern.matcher(parsed[i]).find()) {
                    String stepRangeThrough = null;
                    switch (i) {
                        case 4:
                            stepRangeThrough = "12";
                            break;
                        case 5:
                            stepRangeThrough = "6";
                            break;
                        case 6:
                            stepRangeThrough = String.valueOf(MAX_YEAR);
                            break;
                        default:
                            break;
                    }

                    if (stepRangeThrough != null) {
                        final String[] steps = parsed[i].split("/");
                        if (steps.length > 2) {
                            final CronExpressionPart errorRange;
                            final String fieldString;
                            if (stepRangeThrough.equals("12")) {
                                errorRange = MONTH;
                                fieldString = "MONTH";
                            } else if (stepRangeThrough.equals("6")) {
                                errorRange = DOW;
                                fieldString = "DAY OF WEEK";
                            } else {
                                errorRange = YEAR;
                                fieldString = "YEAR";
                            }

                            throw new CronExpressionParseException(String.format("The expression describing the %s field is not in a valid format", fieldString), errorRange);
                        }

                        parsed[i] = String.format("%d-%d/%d", Integer.parseInt(steps[0]), Integer.parseInt(stepRangeThrough), Integer.parseInt(steps[1]));
                    }
                }
            }
        }

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }


    // Parse options
    public static class Options {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region FIELDS

        // Defaults
        private boolean throwExceptionOnParseError  = true;
        private boolean verbose                     = false;
        private boolean use24HourTimeFormat         = true;
        private boolean useJavaEeScheduleExpression = false;

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region ACCESSORS

        public boolean isThrowExceptionOnParseError() {
            return throwExceptionOnParseError;
        }

        public void setThrowExceptionOnParseError(boolean throwExceptionOnParseError) {
            this.throwExceptionOnParseError = throwExceptionOnParseError;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public boolean isUse24HourTimeFormat() {
            return use24HourTimeFormat;
        }

        public void setUse24HourTimeFormat(boolean use24HourTimeFormat) {
            this.use24HourTimeFormat = use24HourTimeFormat;
        }

        public boolean isUseJavaEeScheduleExpression() {
            return useJavaEeScheduleExpression;
        }

        public void setUseJavaEeScheduleExpression(boolean useJavaEeScheduleExpression) {
            this.useJavaEeScheduleExpression = useJavaEeScheduleExpression;
        }

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region CONSTRUCTORS

        /**
         * Constructor
         */
        public Options() {

        }

        /**
         * \
         * Constructor
         *
         * @param throwExceptionOnParseError  Defaults is TRUE
         * @param verbose                     Defaults is FALSE
         * @param use24HourTimeFormat         Defaults is TRUE
         * @param useJavaEeScheduleExpression Defaults is FALSE
         */
        public Options(final boolean throwExceptionOnParseError, final boolean verbose, final boolean use24HourTimeFormat, final boolean useJavaEeScheduleExpression) {
            this.throwExceptionOnParseError = throwExceptionOnParseError;
            this.verbose = verbose;
            this.use24HourTimeFormat = use24HourTimeFormat;
            this.useJavaEeScheduleExpression = useJavaEeScheduleExpression;
        }

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public abstract class RxReplace {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region FIELDS

        private final Pattern pattern;
        private Matcher matcher;

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region CONSTRUCTORS

        /**
         * Constructor
         *
         * @param regex The regular expression to use for replacement
         */
        public RxReplace(final String regex) {
            this.pattern = Pattern.compile(regex);
        }

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //region METHODS

        /**
         * Overridden to compute a replacement for each match. Use the method 'group' to access the captured groups.
         *
         * @return String The replaced string
         */
        public abstract String replacement();

        /**
         * Returns the input subsequence captured by the given group during the previous match operation.
         *
         * @param i The group index (starting at 0)
         * @return The contents of the requested group
         */
        public String group(int i) {
            return matcher.group(i);
        }

        /**
         * Returns the result of rewriting 'original' by invoking the method 'replacement' for each match of the regular expression supplied to the constructor
         *
         * @param original The original string
         * @return The rewritten string after replacements
         */
        public String replace(final CharSequence original) {
            // Get a matcher for the original pattern
            this.matcher = pattern.matcher(original);

            final StringBuffer result = new StringBuffer(original.length());
            while (matcher.find()) {
                // Discard everything up until the current match (we just want to update the matcher's cursor)
                matcher.appendReplacement(result, "");

                // Perform implemented replacement and append the resulting string to the buffer
                result.append(replacement());
            }

            // Append the rest of the sequence
            matcher.appendTail(result);

            // Return the rewritten string
            return result.toString();
        }

        //endregion
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
}