package itmo.blps.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ListingCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 500, message = "Title must be between 5 and 500 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 255, message = "Region must not exceed 255 characters")
    private String region;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1", message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Area is required")
    @DecimalMin(value = "1", message = "Area must be positive")
    private BigDecimal areaSqm;

    @NotNull(message = "Rooms is required")
    @Min(value = 1, message = "Rooms must be at least 1")
    private Integer rooms;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAreaSqm() {
        return areaSqm;
    }

    public void setAreaSqm(BigDecimal areaSqm) {
        this.areaSqm = areaSqm;
    }

    public Integer getRooms() {
        return rooms;
    }

    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }
}
