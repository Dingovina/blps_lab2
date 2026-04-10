package itmo.blps.specification;

import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import itmo.blps.exception.BadRequestException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Parses a single {@code filter} query-string into a JPA {@link Specification}
 * for {@link Listing}.
 * <p>
 * Parses a list of {@code filter} query-string values into a JPA {@link Specification}.
 * Each value has the form {@code field(op)value}; multiple values are passed as
 * repeated {@code filter=} parameters (Spring MVC collects them into a {@code List}).
 * <ul>
 *   <li>Operators: {@code =  !=  >  >=  <  <=}</li>
 *   <li>Supported fields: price, rooms, areaSqm, region, address</li>
 *   <li>Conditions are joined with AND; status = ACTIVE is always appended.</li>
 * </ul>
 * Example: {@code ?filter=price(>=)1000000&filter=rooms(<=)2&filter=region(=)Москва}
 */
public final class ListingSpecification {

    private static final Set<String> NUMERIC_FIELDS =
            Set.of("price", "rooms", "areaSqm");

    private static final Set<String> STRING_FIELDS =
            Set.of("region", "address");

    private static final Set<String> ALLOWED_FIELDS;

    static {
        ALLOWED_FIELDS = new java.util.HashSet<>();
        ALLOWED_FIELDS.addAll(NUMERIC_FIELDS);
        ALLOWED_FIELDS.addAll(STRING_FIELDS);
    }

    private static final Set<String> VALID_OPERATORS =
            Set.of("=", "!=", ">", ">=", "<", "<=");

    private ListingSpecification() {
    }

    public static Specification<Listing> fromFilter(List<String> filters) {
        Specification<Listing> spec = activeOnly();

        if (filters == null || filters.isEmpty()) {
            return spec;
        }

        for (String raw : filters) {
            if (raw == null || raw.isBlank()) continue;

            String token = raw.trim();
            int open = token.indexOf('(');
            int close = token.indexOf(')');

            if (open < 1 || close < 0 || close <= open + 1
                    || close >= token.length() - 1) {
                throw new BadRequestException(
                        "Invalid filter value, expected field(op)value: " + token);
            }

            String field = token.substring(0, open);
            String op = token.substring(open + 1, close);
            String value = token.substring(close + 1);

            if (!ALLOWED_FIELDS.contains(field)) {
                throw new BadRequestException("Unknown filter field: " + field);
            }
            if (!VALID_OPERATORS.contains(op)) {
                throw new BadRequestException("Unsupported operator: " + op);
            }

            spec = spec.and(fieldPredicate(field, op, value));
        }

        return spec;
    }

    private static Specification<Listing> activeOnly() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), ListingStatus.ACTIVE);
    }

    private static Specification<Listing> fieldPredicate(String field,
                                                         String op,
                                                         String rawValue) {
        if (NUMERIC_FIELDS.contains(field)) {
            return numericPredicate(field, op, rawValue);
        }
        return stringPredicate(field, op, rawValue);
    }

    private static Specification<Listing> numericPredicate(String field,
                                                           String op,
                                                           String rawValue) {
        return (root, query, cb) -> {
            Comparable<?> value = parseNumber(field, rawValue);
            return buildComparison(cb, root, field, op, value);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Predicate buildComparison(CriteriaBuilder cb,
                                             Root<Listing> root,
                                             String field,
                                             String op,
                                             Comparable value) {
        return switch (op) {
            case "=" -> cb.equal(root.get(field), value);
            case "!=" -> cb.notEqual(root.get(field), value);
            case ">" -> cb.greaterThan(root.get(field), value);
            case ">=" -> cb.greaterThanOrEqualTo(root.get(field), value);
            case "<" -> cb.lessThan(root.get(field), value);
            case "<=" -> cb.lessThanOrEqualTo(root.get(field), value);
            default -> throw new BadRequestException("Unsupported operator: " + op);
        };
    }

    private static Comparable<?> parseNumber(String field, String raw) {
        try {
            if ("rooms".equals(field)) {
                return Integer.valueOf(raw);
            }
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new BadRequestException(
                    "Invalid numeric value for field '" + field + "': " + raw);
        }
    }

    private static Specification<Listing> stringPredicate(String field,
                                                          String op,
                                                          String rawValue) {
        return (root, query, cb) -> switch (op) {
            case "=" -> cb.equal(cb.lower(root.get(field)),
                    rawValue.toLowerCase());
            case "!=" -> cb.notEqual(cb.lower(root.get(field)),
                    rawValue.toLowerCase());
            default -> throw new BadRequestException(
                    "Operator '" + op + "' is not supported for text field '" + field + "'");
        };
    }
}
