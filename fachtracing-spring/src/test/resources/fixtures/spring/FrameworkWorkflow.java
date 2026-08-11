package fixture.spring;

import at.gepardec.fachtracing.api.FachTracing;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class FrameworkWorkflow {
    @FachTracing("classify result page")
    public String classify(Page<Entry> page) {
        if (page.isEmpty()) return "no entries";
        if (page.getNumberOfElements() == 1) return "one entry";
        return "multiple entries";
    }

    @FachTracing("search repository records")
    public String search(String prefix, EntryRepository records) {
        Page<Entry> page = records.findByNameStartingWith(prefix, PageRequest.of(0, 5));
        if (page.isEmpty()) return "no entries";
        return "entries found";
    }

    @FachTracing("submit valid record")
    public String submit(
            String name,
            BindingResult validation,
            JpaRepository<Entry, Long> records,
            RedirectAttributes response) {
        if (!StringUtils.hasText(name)) {
            validation.rejectValue("name", "required");
        }
        if (validation.hasErrors()) return "correction required";
        try {
            records.saveAndFlush(new Entry(name));
            response.addFlashAttribute("message", "record saved");
            return "record saved";
        } catch (DataIntegrityViolationException duplicate) {
            validation.rejectValue("name", "duplicate");
            return "duplicate record";
        } catch (IllegalStateException unexpected) {
            return "unexpected failure";
        }
    }

    @FachTracing("unsupported Spring helper")
    public String unsupported(String value) {
        return StringUtils.capitalize(value);
    }

    @FachTracing("unsupported custom page query")
    public String unsupportedCustomQuery(String prefix, EntryRepository records) {
        return records.loadMatchingEntries(prefix, PageRequest.of(0, 5)).isEmpty()
                ? "no entries" : "entries found";
    }

    public interface EntryRepository extends JpaRepository<Entry, Long> {
        Page<Entry> findByNameStartingWith(String prefix, Pageable pageable);
        Page<Entry> loadMatchingEntries(String prefix, Pageable pageable);
    }

    public record Entry(String name) { }
}
